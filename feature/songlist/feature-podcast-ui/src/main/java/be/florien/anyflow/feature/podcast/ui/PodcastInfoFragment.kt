package be.florien.anyflow.feature.podcast.ui

import android.text.Html
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastActionType
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastFieldType
import be.florien.anyflow.feature.podcast.base.ui.BasePodcastInfoFragment
import be.florien.anyflow.feature.podcast.base.ui.R
import be.florien.anyflow.management.podcast.model.PodcastEpisodeInfo

class PodcastInfoFragment(podcastId: Long) :
    BasePodcastInfoFragment<PodcastInfoViewModel>(podcastId = podcastId) {
    override fun getPodcastViewModel(): PodcastInfoViewModel =
        ViewModelProvider(
            this,
            (activity as ViewModelFactoryProvider).viewModelFactory
        )[PodcastInfoViewModel::class.java]

    override fun BasePodcastInfoRow.toInfoRow(): InfoRow {
        return when (this) {
            is BasePodcastInfoRow.ShortcutInfoRow -> InfoRow.ShortcutInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(viewModel.podcastEpisodeInfo.getTextFromField(fieldType)),
                ImageConfig(null, fieldType.iconRes),
                this
            )

            is BasePodcastInfoRow.PodcastDownloadInfoRow -> InfoRow.ProgressInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(viewModel.podcastEpisodeInfo.getTextFromField(fieldType)),
                ImageConfig(null, actionType.iconRes),
                this,
                this.progress.map { it.downloaded.toDouble() / it.total },
                this.progress.map { ((it.downloaded + it.queued).toDouble() / it.total) }
            )

            is BasePodcastInfoRow.PodcastInfoContainerRow -> {
                InfoRow.ContainerInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(viewModel.podcastEpisodeInfo.getTextFromField(fieldType)),
                    ImageConfig(null, fieldType.iconRes),
                    subRows.map { it.toInfoRow() },
                    this
                )

            }

            is BasePodcastInfoRow.PodcastInfoRow -> when (this.actionType) {
                PodcastActionType.InfoTitle -> InfoRow.BasicInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(viewModel.podcastEpisodeInfo.getTextFromField(fieldType)),
                    ImageConfig(null, fieldType.iconRes),
                    this
                )

                PodcastActionType.None -> InfoRow.BasicInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(R.string.info_action_downloaded_description),
                    ImageConfig(null, fieldType.iconRes),
                    this
                )

                PodcastActionType.AddToFilter,
                PodcastActionType.AddNext,
                PodcastActionType.Download,
                PodcastActionType.Search -> InfoRow.ActionInfoRow(
                    actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                    TextConfig(
                        viewModel.podcastEpisodeInfo.getTextFromField(fieldType),
                        actionType.getText()
                    ),
                    ImageConfig(null, actionType.iconRes),
                    this
                )

                else -> throw IllegalArgumentException()
            }

            is BasePodcastInfoRow.PodcastHtmlRow -> InfoRow.BasicInfoRow(
                actionType.titleRes.takeIf { it != 0 } ?: fieldType.titleRes,
                TextConfig(
                        HtmlCompat.fromHtml(
                            viewModel.podcastEpisodeInfo.description,
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        ).toString()
                ),
                ImageConfig(null, fieldType.iconRes),
                this
            )

            else -> {
                throw IllegalStateException()
            }
        }
    }

    private fun PodcastEpisodeInfo.getTextFromField(field: PodcastFieldType): String {
        return try {
            when (field) {
                PodcastFieldType.Title -> title
                PodcastFieldType.Podcast -> podcast
                PodcastFieldType.Description -> Html.escapeHtml(description)
                PodcastFieldType.Publication -> TimeOperations.toDisplayDate(publicationDate)
                PodcastFieldType.Website -> website
                PodcastFieldType.State -> state
                PodcastFieldType.Duration -> TimeOperations.toMediaDuration(time)
            }
        } catch (exception: Exception) {
            ""
        }
    }

    private fun PodcastActionType.getText(): Int? {
        return when (this) {
            PodcastActionType.None,
            PodcastActionType.InfoTitle,
            PodcastActionType.ExpandableTitle -> null

            PodcastActionType.AddToFilter -> R.string.info_action_filter_on
            PodcastActionType.AddNext -> R.string.info_action_track_next
            PodcastActionType.Search -> R.string.info_action_search_on
            PodcastActionType.Download -> R.string.info_action_download_description
        }
    }

}