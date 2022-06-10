package com.shannon.library.music.media

import androidx.annotation.FloatRange
import androidx.annotation.IntDef

/**
 *
 * @ProjectName:    MusicDemo
 * @Package:        com.shannon.library.music.media
 * @ClassName:      MediaController
 * @Description:     java类作用描述
 * @Author:         czhen
 * @CreateDate:     2022/6/8 10:09
 */
interface MediaController {

    companion object {
        const val PLAY_MODE_SHUFFLE = 1
        const val PLAY_MODE_ORDER = 2
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @kotlin.annotation.Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.TYPE
    )
    @IntDef(value = [PLAY_MODE_SHUFFLE, PLAY_MODE_ORDER])
    annotation class PlayMode {}

    fun play()

    fun play(targetPosition: Int)

    fun play(targetPosition: Int, positionMs: Long)

    fun pause()

    fun stop()

    fun seekToPreviousItem()

    fun seekToNextItem()

    fun addMediaSource(sources: List<MediaData>)

    fun setMediaPlayerListener(mediaPlayerListener: MediaPlayerListener)

    fun getCurrentMediaItem(): String

    fun switchPlayMode(): @PlayMode Int

    fun setPlaybackSpeed(
        @FloatRange(
            from = 0.0,
            to = 2.0,
            fromInclusive = false,
            toInclusive = false
        ) speed: Float
    )
}