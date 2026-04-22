package pl.wsei.pam.lab06.data

import android.content.Context
import java.time.LocalDate

interface CurrentDateProvider {
    val currentDate: LocalDate
}

class DefaultDateProvider : CurrentDateProvider {
    override val currentDate: LocalDate
        get() = LocalDate.now()
}

interface AppContainer {
    val todoTaskRepository: TodoTaskRepository
    val currentDateProvider: CurrentDateProvider
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val todoTaskRepository: TodoTaskRepository by lazy {
        DatabaseTodoTaskRepository(AppDatabase.getInstance(context).taskDao())
    }
    override val currentDateProvider: CurrentDateProvider = DefaultDateProvider()
}
