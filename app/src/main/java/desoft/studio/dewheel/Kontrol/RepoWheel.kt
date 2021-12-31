package desoft.studio.dewheel.Kontrol

import android.util.Log
import desoft.studio.dewheel.local.Klocalbase
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepoWheel (private val indaba : Klocalbase )
{
    private val TAG :String = "+++ WHEEL REPOSITORY +++";
    
    suspend fun REPO_LOCAL_INSERT_USER(inuser : Kuser) : Long
    {
        return withContext(Dispatchers.IO){
            var res = indaba.kablesDao().InsertUser(inuser);
            Log.w(TAG, "REPO_LOCAL_INSERT_USER: Row id that is inserted $res");
            res.get(0);
        }
    }

    suspend fun REPO_LOCAL_FIND_USER(inid : String): Kuser?
    {
        return (indaba.kablesDao().FindUser(inid));
    }

}