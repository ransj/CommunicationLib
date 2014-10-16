/**
 * 
 */
package name.kingbright.alljoyn.test;

import name.kingbright.alljoyn.AllJoynGroupService;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * @author KingBright
 * 
 */
public class TestActivity extends Activity {
    AllJoynGroupService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mService = new AllJoynGroupService();
        mService.create();
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn1:
                mService.createGroup("ABC");
                break;
            case R.id.btn2:
                mService.startDiscoverGroup();
                break;
            case R.id.btn3:
                mService.advertise("ABC");
                break;
            case R.id.btn4:
                mService.cancelAdvertise("ABC");
                break;
            case R.id.btn5:
                mService.stopDiscoverGroup();
                break;
            case R.id.btn6:
                mService.joinGroup("ABC");
                break;
            case R.id.btn7:
                mService.leaveGroup("ABC");
                break;
        }
    }
}
