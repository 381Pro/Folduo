# Folduo セキュリティチェック

対象: `main` 相当のコミット `c9e5976`（Folduo 0.1.21）、2026年9月15日実施。
範囲: アプリ本体（`app/src/main`）、Shizuku ユーザーサービス、デバッグ用ソース、`tools/` の補助ツール、ビルド定義、Git 履歴。

## 総評

外部から悪用できる深刻な脆弱性は見つかりませんでした。shell 権限で動く `ShellBridge` は全メソッドで呼び出し元 UID を検証し、画面キャプチャは secure / protected レイヤーを拒否し、公開コンポーネントは必要最小限です。指摘は署名運用と要確認事項が中心です。

## 指摘事項

### 中: リリースビルドがデバッグ署名になっている

- 場所: `app/build.gradle.kts` の `buildTypes { release { signingConfig = signingConfigs.getByName("debug") } }`
- 内容: デバッグ keystore はパスワード `android` が既知で、通常バックアップもされません。ビルド機が侵害されると同じ署名の更新 APK を作られ、既存ユーザーへそのまま上書きインストールできます。逆に keystore を失うと正規の更新が配布できません。
- `docs/publication-audit.md` に「実験用のデバッグ署名」と明記されており意図的ですが、配布を続けるなら専用のリリース keystore を用意し、`local.properties` か環境変数からパスワードを読む構成を推奨します。

### 低（要実機確認）: 最近のアプリのプレビューで FLAG_SECURE ウィンドウが除外される保証がない

- 場所: `TaskDisplayRouter.preview()`
- 内容: `getTaskSnapshot` の結果から `HardwareBuffer.USAGE_PROTECTED_CONTENT` のみ除外しています。これは DRM 保護バッファの検査で、銀行アプリなどが使う `FLAG_SECURE` とは別の仕組みです。同じ用途の `ShellBridge.captureBehind()` は `containsSecureLayers` と `SECURE_CONTENT_POLICY_THROW_EXCEPTION` で二重に防いでいるのに対し、こちらは検査がありません。
- 推奨: 実機で `FLAG_SECURE` を使うアプリ（例: 銀行アプリ、ブラウザのシークレットタブ）を開いた直後に内側ナビの「最近使ったアプリ」を表示し、サムネイルが黒またはアイコンになることを確認してください。内容が見える場合はプレビュー機能をアイコンのみに戻すのが安全です。

### 低: Gradle Wrapper の JAR を公式ハッシュと照合できなかった

- `gradle/wrapper/gradle-wrapper.properties` には `distributionSha256Sum` と `validateDistributionUrl=true` があり、配布物の検証は有効です。
- ただし `gradle-wrapper.jar` 自体はこのセッションから `services.gradle.org` へ到達できず照合できていません。ローカルで次を比較してください。

```text
sha256sum gradle/wrapper/gradle-wrapper.jar
=> 497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7
公式: https://services.gradle.org/distributions/gradle-9.5.1-wrapper.jar.sha256
```

### 低: CI と依存更新の仕組みがない

- `.github/` が存在せず、Dependabot や `gradle/actions/wrapper-validation` がありません。依存は Shizuku API 13.1.5（最新）と JUnit のみで現時点の問題はありませんが、自動検知を推奨します。

### 情報: shell UID からの呼び出しも許可している

- 場所: `ShellBridge.authorize()`
- 内容: 自アプリの UID に加えて `Process.myUid()`（uid 2000 = shell）も許可しています。デバッグ用の `ShellSmoke` / `PanelProbe` がプロセス内で直接呼ぶために必要ですが、Binder を入手できれば adb shell や他の Shizuku アプリのサービスからも呼べます。Binder は Shizuku が要求元パッケージにしか渡さないため実質的なリスクは低く、shell 権限を持つ攻撃者は既に同等以上の操作が可能です。

### 情報: 運用上の注意

- README の「ワイヤレスデバッグで Shizuku を起動」運用は、ペアリング済みのホストからいつでも ADB 接続できる状態を維持します。信頼できるネットワーク以外では無効にする旨を README に添えると親切です。
- `TaskDisplayRouter.launchSelected()` は `Log.i("FolduoLaunch", ...)` にタスク ID と表示先を出力します。アプリ名や画面内容は含まれません。

## 問題なしと確認した項目

| 項目 | 確認内容 |
| --- | --- |
| Manifest | 公開コンポーネントは `MainActivity`（LAUNCHER）、`HomeActivity`（HOME）、`ShizukuProvider`（`INTERACT_ACROSS_USERS_FULL` で保護、Shizuku 標準）のみ。`MotionService` と `RestartReceiver` は非公開 |
| データ保護 | `allowBackup=false`、`dataExtractionRules` で全ドメイン除外、`usesCleartextTraffic=false`、`INTERNET` 権限なし |
| Intent 処理 | `HomeActivity` と `MainActivity` は外部 Intent の extras を読まない。起動対象は `AppCatalog` が `queryIntentActivities` で得た component のみ |
| ShellBridge | 全メソッドで `authorize()`、`clearCallingIdentity` と `restoreCallingIdentity` が対。`launchApp` は exported かつ enabled な LAUNCHER activity のみ許可。`moveApp` は display 0/1 限定、`navigate` と `launchApp` は display 1 限定 |
| 画面キャプチャ | secure / protected レイヤーを THROW ポリシーと `containsSecureLayers` で拒否。Bitmap はメモリのみで、ディスクやログへ書かない |
| ステータスバー | `IStatusBarService.disable` は時計・通知アイコン・システム情報のみ。通知シェード、ナビ、プライバシーインジケーターは無効化しない |
| PendingIntent | すべて `FLAG_IMMUTABLE` |
| 保存データ | `SharedPreferences` は `MODE_PRIVATE`。内容は有効フラグ、所有 PID、メモ、お気に入り component 名のみ。`UiText.decode` は自パッケージの string リソース名解決に限定し、入れ子は深さ 8 まで |
| オーバーレイ | 全画面の凍結画像はタッチを消費するが、1400 ms 以内に描画が確定しなければ `fail()` で除去。恒久的なフリーズは防止 |
| 補助ツール | `cover-wallpaper.py` は `subprocess` をリスト引数で呼び、`shlex.quote` 済み、`shell=True` なし。`CoverWallpaperSetup` は uid 2000 と機種 `SM-F966Z` を検証し、未知の壁紙は上書きしない |
| 秘密情報 | ソースと Git 履歴（9 コミット）に鍵、トークン、パスワード、端末シリアルなし。`.gitignore` で keystore、`.env`、`local.properties` を除外 |
| 依存関係 | Shizuku API / Provider 13.1.5（最新）、JUnit 4.13.2、AndroidX test のみ |

## 確認の限界

- 実機（Galaxy Z Fold7）での動作確認は行っていません。FLAG_SECURE のプレビューは実機確認が必要です。
- Gradle Wrapper JAR の公式ハッシュ照合はネットワーク制限で未実施です。
- 静的な読解によるレビューで、動的解析やファジングは行っていません。
