package com.appspell.wildscroll.sections

import android.support.v4.util.ArrayMap
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import com.appspell.wildscroll.adapter.SectionFastScroll
import com.eatigo.common.coroutines.Android
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

interface OnSectionChangedListener {
    fun onSectionChanged()
}

data class SectionInfo(val name: String,
                       val shortName: Char,
                       val position: Int,
                       val count: Int)

class Sections {
    companion object {
        const val UNSELECTED = -1

        private const val SECTION_SHORT_NAME_EMPTY = '-'
        private const val SECTION_SHORT_NAME_DIGITAL = '#'
    }

    var left = 0f
    var top = 0f
    var width = 0f
    var height = 0f

    var gravity = Gravity.RIGHT
    var collapseDigital = true

    var paddingLeft = 0f
    var paddingRight = 0f

    var sections: ArrayMap<Char, SectionInfo> = ArrayMap()

    var selected = UNSELECTED //TODO redraw section bar after set new value

    private var job: Job? = null

    fun getCount() = sections.size

    fun changeSize(w: Int, h: Int, highlightTextSize: Float) {
        val sectionCount = sections.size
        width = highlightTextSize + paddingLeft + paddingRight
        height = h / sectionCount.toFloat()

        when (gravity) {
            Gravity.START, Gravity.LEFT -> left = 0f
            Gravity.END, Gravity.RIGHT -> left = w - width
        //TODO top / bottom
        }
    }

    fun contains(x: Float, y: Float): Boolean {
        return x >= left &&
                x <= left + width &&
                y >= top &&
                y <= height * sections.size
    }


    fun getSectionInfoByIndex(index: Int): SectionInfo? {
        val key = getSectionByIndex(index)
        return sections[key]
    }

    fun createShortName(name: String): Char =
            when {
                name.isEmpty() -> SECTION_SHORT_NAME_EMPTY
                collapseDigital && TextUtils.isDigitsOnly(name[0].toString()) -> SECTION_SHORT_NAME_DIGITAL
                else -> name[0].toUpperCase()
            }

    fun refresh(adapter: RecyclerView.Adapter<*>?, listener: OnSectionChangedListener) { //TODO move it to separate class
        job?.cancel()
        job = launch(Android) {
            sections = fetchSections(adapter).await()
            listener.onSectionChanged()
        }
    }

    private fun fetchSections(adapter: RecyclerView.Adapter<*>?): Deferred<ArrayMap<Char, SectionInfo>> {
        return async(CommonPool) {
            val map = ArrayMap<Char, SectionInfo>()

            if (adapter == null) {
                return@async map
            }
            if (adapter.itemCount <= 1 || adapter !is SectionFastScroll) {
                return@async map
            }

            if (adapter.itemCount > 0) {
                for (position in 0 until adapter.itemCount) {

                    val name = adapter.getSectionName(position)

                    val shortName = createShortName(name)

                    val sectionInfo =
                            if (map.containsKey(shortName)) map[shortName]!!.copy(count = map[shortName]!!.count + 1)
                            else SectionInfo(name, shortName, position, 1)

                    map.put(shortName, sectionInfo)
                }
            }
            return@async map
        }
    }

    protected fun getSectionByIndex(index: Int): Char = sections.keyAt(index)
}
