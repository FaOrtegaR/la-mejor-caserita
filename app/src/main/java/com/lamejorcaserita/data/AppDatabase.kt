package com.lamejorcaserita.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lamejorcaserita.data.dao.PedidoDao
import com.lamejorcaserita.data.dao.ProductoDao
import com.lamejorcaserita.data.entity.Pedido
import com.lamejorcaserita.data.entity.PedidoItem
import com.lamejorcaserita.data.entity.Producto
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Producto::class, Pedido::class, PedidoItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de la versión 1 a la 2
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pedidos ADD COLUMN pagado INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun obtenerBaseDeDatos(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lamejorcaseria_database"
                )
                    .addMigrations(MIGRACION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}