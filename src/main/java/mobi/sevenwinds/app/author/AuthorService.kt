package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object AuthorService {
    suspend fun create(request: AuthorRequest): AuthorResponse = withContext(Dispatchers.IO) {
            transaction {
                val entity = AuthorEntity.new {
                    fullName = request.fullName
                    createdAt = DateTime.now()
                }
                AuthorResponse(
                    entity.id.value,
                    entity.fullName,
                    entity.createdAt.toString()
                )
            }
        }
}