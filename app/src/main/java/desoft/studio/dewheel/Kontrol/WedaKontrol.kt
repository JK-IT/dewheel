package desoft.studio.dewheel.Kontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import desoft.studio.dewheel.local.Kuser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WedaKontrol(private val repo: RepoWheel) :  ViewModel()
{
    private val iodis = Dispatchers.IO;

    // + --------->>-------->>--------->>*** -->>----------->>>>
    fun VM_ADD_USER_LOCAL(inuser : Kuser)
    {
        viewModelScope.launch(iodis) {
            repo.REPO_LOCAL_INSERT_USER(inuser);
        }
    }


    // + --------->>-------->>--------->>*** -->>----------->>>>
    // * DATA WHEEL KONTROL FACTORY
    class DataWheelKontrolFactory(private val repoWheel: RepoWheel) : ViewModelProvider.Factory
    {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(WedaKontrol::class.java))
            {
                return WedaKontrol(repoWheel) as T;
            }
            throw IllegalArgumentException("Unknown View Model class");
        }

    }
}