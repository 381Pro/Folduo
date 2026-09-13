package jp.bunkaich.sukashimotion;

import org.junit.Test;
import static org.junit.Assert.*;

public class DisplayRequestOwnerTest {
    private final String dump="Registered processes: size=2\nOverride Request active: true\nRequest: mPid=26937, mRequestedState=5, mFlags=0, mStatus=ACTIVE\n";
    @Test public void recoversOnlyRecordedDeadOwner(){assertTrue(DisplayRequestOwner.canRecover(dump,26937,5,false));}
    @Test public void neverTakesOverLiveController(){assertFalse(DisplayRequestOwner.canRecover(dump,26937,5,true));}
    @Test public void neverTakesOverForeignController(){assertFalse(DisplayRequestOwner.canRecover(dump,1234,5,false));}
    @Test public void requiresRecordedOwnerAndExpectedState(){assertFalse(DisplayRequestOwner.canRecover(dump,0,5,false));assertFalse(DisplayRequestOwner.canRecover(dump,26937,4,false));}
    @Test public void ignoresInactiveOrUnrecognizedDump(){assertFalse(DisplayRequestOwner.canRecover(dump.replace("active: true","active: false"),26937,5,false));assertFalse(DisplayRequestOwner.canRecover("",26937,5,false));}
}
