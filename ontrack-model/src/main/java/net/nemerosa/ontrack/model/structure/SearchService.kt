package net.nemerosa.ontrack.model.structure

import net.nemerosa.ontrack.model.Ack

/**
 * Service to search based on text
 */
interface SearchService {

    /**
     * Gets the list of types of search
     */
    val searchResultTypes: List<SearchResultType>

    /**
     * Paginated search entry point based on registered search indexers.
     */
    fun paginatedSearch(request: SearchRequest): SearchResults

    /**
     *
     */
    fun rawSearch(
            token: String,
            indexName: String?,
            offset: Int = 0,
            size: Int = 10,
    ): SearchNodeResults

    /**
     * Makes sure all search indexes are initialized.
     */
    fun indexInit()

    /**
     * Resetting all search indexes, optionally restoring them.
     *
     *
     * This method is mostly used for testing but could be used
     * to reset faulty indexes.
     *
     * @param reindex `true` to relaunch the indexation afterwards
     * @return OK if indexation was completed successfully
     */
    fun indexReset(reindex: Boolean): Ack

}