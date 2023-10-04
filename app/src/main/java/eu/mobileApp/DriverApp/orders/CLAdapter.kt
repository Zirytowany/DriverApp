package eu.mobileApp.DriverApp.orders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import eu.mobileApp.DriverApp.databinding.ListGroupBinding
import eu.mobileApp.DriverApp.databinding.ListItemBinding

class CLAdapter internal constructor(
    private val context: Context,
    private val titleList: List<String>,
    private val dataList: HashMap<String, List<String>>
): BaseExpandableListAdapter() {

    private val inflater = LayoutInflater.from(context)
    private lateinit var groupBinding:ListGroupBinding
    private lateinit var itemBinding: ListItemBinding

    override fun getGroupCount(): Int {
        return this.titleList.size
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[this.titleList[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.titleList[listPosition]
    }

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[this.titleList[listPosition]]!![expandedListPosition]
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, view: View?, parent: ViewGroup?): View {
        var convertView=view
        var holder: GroupViewHolder
        if (convertView==null){
            groupBinding= ListGroupBinding.inflate(inflater)
            convertView = groupBinding.root
            holder=GroupViewHolder()
            holder.label=groupBinding.listTitle
            convertView.tag=holder
        }else{
            holder=convertView.tag as GroupViewHolder
        }
        val listTitle = getGroup(listPosition) as String
        holder.label!!.text=listTitle
        return convertView
    }

    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, view: View?, parent: ViewGroup?): View {
        var convertView = view
        val holder: ItemViewHolder
        if(convertView == null){
            itemBinding= ListItemBinding.inflate(inflater)
            convertView=itemBinding.root
            holder=ItemViewHolder()
            holder.label=itemBinding.expandedListItem
            convertView.tag=holder
        }else{
            holder=convertView.tag as ItemViewHolder
        }
        val expandedListText=getChild(listPosition, expandedListPosition)
        holder.label!!.text= expandedListText.toString()
        return convertView
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    inner class ItemViewHolder{
        internal var label: TextView?=null
    }

    inner class GroupViewHolder{
        internal var label: TextView?=null
    }

}