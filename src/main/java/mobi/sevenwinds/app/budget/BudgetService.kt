package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.authorId = body.authorId?.let { EntityID(it, AuthorTable) }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            // Запрос для данных (с пагинацией) и сортировкой: сначала по месяцу (возрастание), затем по сумме (убывание)
            val query = BudgetTable
                .leftJoin(AuthorTable)
                .select { BudgetTable.year eq param.year }
                .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                .limit(param.limit, param.offset)

            // Отдельный запрос для общего количества (без пагинации)
            val totalQuery = BudgetTable
                .leftJoin(AuthorTable)
                .select { BudgetTable.year eq param.year }

            val total = totalQuery.count()  // теперь корректное общее число
            val data = BudgetEntity.wrapRows(query).map { it.toResponse() }

            // Статистика по ВСЕМ записям года
            val allRecords = BudgetEntity.wrapRows(totalQuery).map { it.toResponse() }
            val sumByType = allRecords.groupBy { it.type.name }.mapValues {
                it.value.sumOf { v -> v.amount }
            }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}