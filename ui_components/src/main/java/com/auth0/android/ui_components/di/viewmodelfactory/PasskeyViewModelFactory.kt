//package com.auth0.android.ui_components.di.viewmodelfactory
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.auth0.android.ui_components.domain.DispatcherProvider
//import com.auth0.android.ui_components.domain.repository.MyAccountRepository
//import com.auth0.android.ui_components.presentation.ui.passkeys.PasskeyViewModel
//
///**
// * Factory for creating PasskeyViewModel with dependencies
// */
//class PasskeyViewModelFactory(
//    private val repository: MyAccountRepository,
//    private val dispatcherProvider: DispatcherProvider
//) : ViewModelProvider.Factory {
//
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return when {
//            modelClass.isAssignableFrom(PasskeyViewModel::class.java) -> {
//                PasskeyViewModel(
//                    repository = repository,
//                    dispatcherProvider = dispatcherProvider
//                ) as T
//            }
//
//            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
//        }
//    }
//}
