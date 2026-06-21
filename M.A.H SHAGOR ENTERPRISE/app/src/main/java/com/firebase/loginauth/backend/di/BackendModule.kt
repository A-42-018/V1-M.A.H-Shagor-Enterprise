package com.firebase.loginauth.backend.di

import android.content.Context
import com.firebase.loginauth.backend.auth.AuthManager
import com.firebase.loginauth.backend.repositories.*
import com.firebase.loginauth.backend.sync.BackgroundSyncManager
import com.firebase.loginauth.database.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideStockBackendRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): StockBackendRepository {
        return StockBackendRepository(context, firestore, auth)
    }
    
    @Provides
    @Singleton
    fun provideOrderBackendRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): OrderBackendRepository {
        return OrderBackendRepository(context, firestore, auth)
    }
    
    @Provides
    @Singleton
    fun provideSellsBackendRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): SellsBackendRepository {
        return SellsBackendRepository(context, firestore, auth)
    }
    
    @Provides
    @Singleton
    fun provideDeliveryBackendRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        stockRepository: StockBackendRepository,
        sellsRepository: SellsBackendRepository
    ): DeliveryBackendRepository {
        return DeliveryBackendRepository(context, firestore, auth, stockRepository, sellsRepository)
    }
    
    @Provides
    @Singleton
    fun provideDueAccountBackendRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): DueAccountBackendRepository {
        return DueAccountBackendRepository(context, firestore, auth)
    }
    
    @Provides
    @Singleton
    fun provideBackgroundSyncManager(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        stockRepository: StockBackendRepository,
        orderRepository: OrderBackendRepository,
        deliveryRepository: DeliveryBackendRepository,
        sellsRepository: SellsBackendRepository,
        dueAccountRepository: DueAccountBackendRepository
    ): BackgroundSyncManager {
        return BackgroundSyncManager(
            context, auth, stockRepository, orderRepository, 
            deliveryRepository, sellsRepository, dueAccountRepository
        )
    }
    
    @Provides
    @Singleton
    fun provideAuthManager(
        auth: FirebaseAuth,
        syncManager: BackgroundSyncManager
    ): AuthManager {
        return AuthManager(auth, syncManager)
    }
}
