package fr.lotfirais.bbchat.adapter;

import android.app.Activity;
import android.view.LayoutInflater; // Add this import
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import fr.lotfirais.bbchat.R; // Ensure R is imported
import fr.lotfirais.bbchat.model.ChatMessage; // Import from the new package

public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {
    private final Activity context; // Store context for getLayoutInflater()

    public ChatMessageAdapter(Activity context, ArrayList<ChatMessage> messages) {
        super(context, 0, messages);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.message_item, parent, false);
        }

        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        messageTextView.setText(message.getText());

        if (message.getType() == ChatMessage.Type.USER) {
            messageTextView.setBackgroundResource(R.drawable.message_background_user);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            } else {
                messageTextView.setGravity(android.view.Gravity.RIGHT);
            }
            ((LinearLayout.LayoutParams) messageTextView.getLayoutParams()).gravity = android.view.Gravity.RIGHT;
            messageTextView.setTextColor(0xFFFFFFFF);
        } else { // BOT
            messageTextView.setBackgroundResource(R.drawable.message_background_bot);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            } else {
                messageTextView.setGravity(android.view.Gravity.LEFT);
            }
            ((LinearLayout.LayoutParams) messageTextView.getLayoutParams()).gravity = android.view.Gravity.LEFT;
            messageTextView.setTextColor(0xFFFFFFFF);
        }

        return convertView;
    }
}