package com.it10x.foodappgstav7_04.ui.cart

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.it10x.foodappgstav7_04.data.pos.repository.CartRepository
import com.it10x.foodappgstav7_04.domain.usecase.TableReleaseUseCase

class CartViewModelFactory(
    private val repository: CartRepository,
    private val tableReleaseUseCase: TableReleaseUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            val handle: SavedStateHandle = extras.createSavedStateHandle()
            return CartViewModel(
                repository = repository,
                tableReleaseUseCase = tableReleaseUseCase,
                savedStateHandle = handle
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
