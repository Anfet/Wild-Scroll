package com.appspell.wildscroll.adapter

import androidx.recyclerview.widget.RecyclerView
import com.appspell.wildscroll.sections.SectionInfo

abstract class SectionFastScrollAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>(), SectionFastScroll {

    var sections: Map<Int, SectionInfo> = emptyMap()
        set(value) {
            field = value
            value.values.forEach { section ->
                notifyItemChanged(section.position)
            }
        }

    open fun isSection(position: Int): Boolean = sections.containsKey(position)

    fun getSectionInfo(position: Int) = sections[position]
}