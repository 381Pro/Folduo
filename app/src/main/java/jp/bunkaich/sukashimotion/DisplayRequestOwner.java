package jp.bunkaich.sukashimotion;

import java.util.regex.*;

/** Accept only the exact dead process whose successful request this app recorded. */
final class DisplayRequestOwner {
    static boolean canRecover(String dump,int previousPid,int desiredState,boolean previousAlive){
        if(previousPid<=0||previousAlive)return false;
        Matcher request=Pattern.compile("Override Request active: true\\s+Request: mPid=(\\d+), mRequestedState=(\\d+), mFlags=\\d+, mStatus=ACTIVE").matcher(dump);
        return request.find()&&Integer.parseInt(request.group(1))==previousPid&&Integer.parseInt(request.group(2))==desiredState;
    }
}
