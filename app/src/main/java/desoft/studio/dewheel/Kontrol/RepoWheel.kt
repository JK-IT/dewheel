package desoft.studio.dewheel.Kontrol

import android.util.Log
import desoft.studio.dewheel.local.Klocalbase
import desoft.studio.dewheel.local.Kuser

class RepoWheel (private val indaba : Klocalbase )
{
    private val TAG :String = "+++ WHEEL REPOSITORY +++";
    
    suspend fun REPO_LOCAL_INSERT_USER(inuser : Kuser)
    {
        var res = indaba.kablesDao().InsertUser(inuser);
        Log.w(TAG, "REPO_LOCAL_INSERT_USER: Row id that is inserted $res");
    }

}