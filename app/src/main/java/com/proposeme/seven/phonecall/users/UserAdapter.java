package com.proposeme.seven.phonecall.users;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.proposeme.seven.phonecall.R;

import java.util.List;

/**
 * Describe: 显示在线用户列表的适配器
 */
public class UserAdapter extends ArrayAdapter<User> {
    private int resourceId;
    public UserAdapter(Context context, int textViewResourceId, List<User> objects){
        super(context,textViewResourceId,objects);
        resourceId = textViewResourceId;
    }
    public View getView(int position, View convertView, ViewGroup parent){
        User user = getItem(position);
        View view;
        ViewHolder viewHolder;
        if(convertView==null){
            view= LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
            viewHolder=new ViewHolder();
            viewHolder.username = (TextView) view.findViewById(R.id.username);
            viewHolder.ip = (TextView) view.findViewById(R.id.ip);
            view.setTag(viewHolder);
        }else {
            view=convertView;
            viewHolder=(ViewHolder)view.getTag();
        }
        viewHolder.username.setText(user.getUsername());
        viewHolder.ip.setText(user.getIp());
        return view;
    }
    class ViewHolder{
        TextView username;
        TextView ip;
    }
}
