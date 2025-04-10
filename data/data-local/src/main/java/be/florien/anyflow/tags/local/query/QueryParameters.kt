package be.florien.anyflow.tags.local.query

import be.florien.anyflow.tags.local.DbSchema
import be.florien.anyflow.tags.local.Equivalent
import be.florien.anyflow.tags.local.TableSchema
import be.florien.anyflow.tags.local.getEquivalent
import be.florien.anyflow.tags.local.getPathToAtom

data class QueryParameters(
    val selects: List<Select>,
    val wheres: List<List<Where>>,
    val orders: List<Order>
) {
    // region Public

    fun getClauses(): Clauses {
        // 3 kind of situations:
        // the schemas don't have any equivalent (just take it);
        val schemasFromMandatory = getMandatorySchemas()
        // the schemas have an equivalent with a table that correspond to a schema's table with no equivalent;
        val mandatoryTables = schemasFromMandatory.getTables()
        val equivalentSchemasFromMandatory = getEquivalentFromTables(mandatoryTables)
        // the rest that could freely be chosen, but we want to select the minimal amount of tables.
        val equivalentSchemasFromMinimal = getEquivalentSchemasFromMinimal(mandatoryTables)

        val finalSchemaSet = QueryParameters(
            selects = schemasFromMandatory.selects + equivalentSchemasFromMandatory.selects + equivalentSchemasFromMinimal.selects,
            wheres = schemasFromMandatory.wheres + equivalentSchemasFromMandatory.wheres + equivalentSchemasFromMinimal.wheres,
            orders = schemasFromMandatory.orders + equivalentSchemasFromMandatory.orders + equivalentSchemasFromMinimal.orders,
        )
        val whereTablesAliasesCount = finalSchemaSet.wheres
            .flatMap { whereList ->
                whereList
                    .map { it.schema }
                    .groupBy { it.table }
                    .filter { it.value.size > 1 && !it.key.isAtom }
                    .flatMap { it.value }
            }
            .groupBy { it.table }
            .mapValues { 0 }
            .toMutableMap()

        val selectComputed = finalSchemaSet.selects.map {
            val selectedSchema = it.schema.pathToAtom
            val mandatorySchema =
                if (selectedSchema != null && mandatoryTables.contains(selectedSchema.table)) {
                    selectedSchema
                } else {
                    it.schema
                }
            Select(mandatorySchema, it.alias)
        }
        val whereComputed = finalSchemaSet.wheres
            .mapNotNull { whereList ->
                val whereTablesAliasesCountCopy =
                    whereTablesAliasesCount.keys.associateWith { 0 }.toMutableMap()
                val whereMapped = whereList.takeIf { it.isNotEmpty() }
                    ?.map {
                        val currentAliasCount = whereTablesAliasesCountCopy[it.schema.table]
                        val activeSchema = it.schema.getActiveSchema()
                        if (currentAliasCount != null) {
                            whereTablesAliasesCountCopy[it.schema.table] = currentAliasCount + 1
                        }
                        Where(activeSchema, it.value)
                    }
                    ?.sortedByDescending { it.schema.table.tableWeight }

                whereTablesAliasesCount.keys.forEach {
                    whereTablesAliasesCount[it] =
                        maxOf(whereTablesAliasesCount[it]!!, whereTablesAliasesCountCopy[it]!!)
                }

                whereMapped
            }
            .distinct()
        val orderComputed = finalSchemaSet
            .orders
            .map { Order(it.schema) }
        val tables =
            selectComputed.map { it.schema.table } + whereComputed.flatMap { it.map { it.schema.table } } + orderComputed.map { it.schema.table }
                .distinct()
        val tableAndAliases = tables.flatMap { table ->
            val aliasCount = whereTablesAliasesCount[table]
            if (aliasCount != null) {
                (0 until aliasCount).map {
                    if (it == 0) {
                        TableAndAlias(table)
                    } else {
                        TableAndAlias(table, table.tableName + (it - 1))
                    }
                }
            } else {
                listOf(TableAndAlias(table))
            }
        }.sortedByDescending { it.tableSchema.tableWeight }
        val joinParameters = tableAndAliases.map { tableAndAlias ->
            val dbSchema = tableAndAlias.tableSchema.getPathToAtom()
            JoinParameter(dbSchema, tableAndAlias.alias)
        }.distinct()
        return Clauses(
            selects = selectComputed,
            wheres = whereComputed,
            orders = orderComputed,
            joins = joinParameters
        )
    }

    //endregion

    //region Private returns QueryParameters

    private fun getMandatorySchemas() = QueryParameters(
        selects = selects.noEquivalents(),
        wheres = wheres.mapNotNull {where -> where.noEquivalents().takeIf { it.isNotEmpty() } },
        orders = orders.noEquivalents()
    )

    private fun getEquivalentFromTables(mandatoryTables: List<TableSchema>) =
        QueryParameters(
            selects = selects
                .getEquivalentFromTables(mandatoryTables)
                .map { it.first.copy(schema = it.second) },
            wheres = wheres.mapNotNull { whereList ->
                whereList
                    .getEquivalentFromTables(mandatoryTables)
                    .map { it.first.copy(schema = it.second) }
                    .takeIf { it.isNotEmpty() }
            },
            orders = orders
                .getEquivalentFromTables(mandatoryTables)
                .map { it.first.copy(schema = it.second) },
        )

    private fun getEquivalentSchemasFromMinimal(mandatoryTables: List<TableSchema>): QueryParameters {
        val equivalentSchemaNotMandatory = getEquivalentSchemasNotFromTables(mandatoryTables)
        val equivalentNotMandatory = equivalentSchemaNotMandatory.getAllEquivalents()
        val minimalSchemaSubset = equivalentNotMandatory.takeMinimalSchemaSubset()
        return equivalentSchemaNotMandatory.getEquivalentSchemasFromSubset(minimalSchemaSubset)
    }

    private fun getEquivalentSchemasNotFromTables(mandatoryTables: List<TableSchema>) =
        QueryParameters(
            selects = selects.notFromTables(mandatoryTables),
            wheres = wheres.mapNotNull { whereList ->
                whereList
                    .notFromTables(mandatoryTables)
                    .takeIf { it.isNotEmpty() }
            },
            orders = orders.notFromTables(mandatoryTables),
        )

    private fun getEquivalentSchemasFromSubset(minimalSchemaSubset: List<DbSchema>) =
        QueryParameters(
            selects = selects
                .mapToSchemaFromEquivalentList(minimalSchemaSubset)
                .map { it.first.copy(schema = it.second) },
            wheres = wheres.mapNotNull { whereList ->
                whereList.mapToSchemaFromEquivalentList(minimalSchemaSubset)
                    .map { it.first.copy(schema = it.second) }
                    .takeIf { it.isNotEmpty() }
            },
            orders = orders.mapToSchemaFromEquivalentList(minimalSchemaSubset)
                .map { it.first.copy(schema = it.second) }

        )

    private fun <T : DbSchemaContainer> List<T>.mapToSchemaFromEquivalentList(
        minimalSchemaSubset: List<DbSchema>
    ) = map { select ->
        val selectedSchema = select
            .schema
            .getEquivalent()!!
            .equivalents
            .first { minimalSchemaSubset.contains(it) }
        select to selectedSchema
    }
    //endregion

    //region Private returns Table

    private fun getTables(): List<TableSchema> =
        (selects.map { it.schema.table } +
                wheres.flatten().map { it.schema.table } +
                orders.map { it.schema.table })
            .distinct()
    //endregion

    //region Private returns Equivalent

    private fun getAllEquivalents(): List<Equivalent> =
        selects.mapNotNull { it.schema.getEquivalent() } +
                wheres.flatten().mapNotNull { it.schema.getEquivalent() } +
                orders.mapNotNull { it.schema.getEquivalent() }
    //endregion

    //region Private returns Schema

    private fun DbSchema.getActiveSchema(): DbSchema = pathToAtom ?: this

    private fun <T : DbSchemaContainer> List<T>.getEquivalentFromTables(
        mandatoryTables: List<TableSchema>
    ): List<Pair<T, DbSchema>> =
        filter { schemaContainer ->
            val equivalent = schemaContainer.schema.getEquivalent()
            equivalent != null && mandatoryTables.intersect(equivalent.equivalents.map { it.table }
                .toSet()).isNotEmpty()
        }.map { schemaContainer ->
            val equivalent = schemaContainer.schema.getEquivalent() ?: throw IllegalStateException()
            val schema = equivalent.equivalents.first { mandatoryTables.contains(it.table) }
            schemaContainer to schema
        }

    private fun List<Equivalent>.takeMinimalSchemaSubset(): List<DbSchema> = map { equivalent ->
        equivalent.equivalents.firstOrNull { schema ->
            all { equivalentFromAll ->
                val tableSchemaList = equivalentFromAll.equivalents.map { it.table }
                tableSchemaList.contains(schema.table)
            }
        }
            ?: equivalent.equivalents.maxBy { schema ->
                count { equivalentFromCount ->
                    val tableSchemaList = equivalentFromCount.equivalents.map { it.table }
                    tableSchemaList.contains(schema.table)
                }
            }
    }
    //endregion

    //region Private filters

    private fun <T : DbSchemaContainer> List<T>.noEquivalents() =
        filter { it.schema.getEquivalent() == null }

    private fun <T : DbSchemaContainer> List<T>.notFromTables(tables: List<TableSchema>) =
        filter {
            val equivalent = it.schema.getEquivalent()
            equivalent != null && tables.intersect(equivalent.equivalents.map { it.table }
                .toSet()).isEmpty()
        }
    //endregion

    //region classes

    interface DbSchemaContainer {
        val schema: DbSchema
    }

    data class Select(
        override val schema: DbSchema,
        val alias: String? = null
    ) : DbSchemaContainer

    data class Where(
        override val schema: DbSchema,
        val value: String
    ) : DbSchemaContainer

    data class Order(
        override val schema: DbSchema,
    ) : DbSchemaContainer

    data class JoinParameter(
        val schema: DbSchema,
        val tableAlias: String? = null
    )

    data class Clauses(
        val selects: List<Select>,
        val wheres: List<List<Where>>,
        val orders: List<Order>,
        val joins: List<JoinParameter>
    )

    data class TableAndAlias(
        val tableSchema: TableSchema,
        val alias: String? = null
    )
    //endregion
}