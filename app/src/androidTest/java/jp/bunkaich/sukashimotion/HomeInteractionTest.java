package jp.bunkaich.sukashimotion;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class HomeInteractionTest {
    @Test public void innerIconsKeepTapAndLongPressSeparate() {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        instrumentation.runOnMainSync(() -> {
            int[] launches = {0}, selected = {-1};
            AppCatalog.App app = new AppCatalog.App("Calculator", new ComponentName("calculator", "calculator.Main"), new ColorDrawable(0xff00aa44));
            HomeScene scene = new HomeScene(context, new HomeScene.Actions() {
                public void launch(AppCatalog.App chosen) { assertEquals(app, chosen); launches[0]++; }
                public void choose(int slot) { selected[0] = slot; }
                public void drawer() {}
                public void settings() {}
                public void note() {}
            });
            scene.updateApps(List.of(app));
            for (boolean inner : new boolean[]{false, true, false, true}) {
                int width = inner ? 1968 : 1080;
                scene.setFold(inner, 0, false);
                scene.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2184, View.MeasureSpec.EXACTLY));
                scene.layout(0, 0, width, 2184);
                int before = launches[0];
                assertTrue(scene.primary.tiles[0].performLongClick());
                assertEquals(0, selected[0]); assertEquals(before, launches[0]);
                assertTrue(scene.primary.tiles[0].performClick());
                assertEquals(before + 1, launches[0]);
                assertEquals(inner ? width / 2 : 0, scene.primary.getLeft());
            }
        });
    }
}
