package edu.tdt.appstudent2.actitities.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.tdt.appstudent2.R;
import edu.tdt.appstudent2.adapters.chat.ChatAdapter;
import edu.tdt.appstudent2.models.User;
import edu.tdt.appstudent2.models.firebase.Admin;
import edu.tdt.appstudent2.models.firebase.Chat;
import edu.tdt.appstudent2.models.firebase.ChatIn;
import edu.tdt.appstudent2.models.firebase.ChatOut;
import io.realm.Realm;

public class ChatActivity extends AppCompatActivity {
    private Realm realm;
    private User user;

    private RecyclerView mMessageRecycler;
    private ChatAdapter mMessageAdapter;

    private EditText edtChat;
    private Button btnChat;

    private DatabaseReference mDatabase;
    private DatabaseReference chatReference;
    private DatabaseReference adminReference;
    private ValueEventListener valueChatEventListener;
    private ValueEventListener valueAdminEventListener;


    private String username;

    private boolean firstLoad = true;
    private boolean isAdmin = false;
    private Admin admin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        realm = Realm.getDefaultInstance();
        user = realm.where(User.class).findFirst();

        username = user.getUserName();

        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new ChatAdapter(this);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        edtChat = (EditText) findViewById(R.id.edittext_chatbox);
        btnChat = (Button) findViewById(R.id.button_chatbox_send);

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = edtChat.getText().toString().trim();
                if(!"".equals(text)){
                    Chat chat = new ChatOut();
                    chat.body = text;
                    chat.time = System.currentTimeMillis();
                    chat.mssv = isAdmin?admin.nickname:user.getUserName();
                    chatReference.push().setValue(chat, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            edtChat.setText("");
                            mMessageRecycler.scrollToPosition((int)mMessageAdapter.chats.size() - 1);
                        }
                    });
                }
            }
        });


        mDatabase = FirebaseDatabase.getInstance().getReference();
        chatReference = mDatabase.child("Chat");
        valueChatEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mMessageAdapter.chats.clear();
                Chat chatGet = null;
                Chat chat = null;
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    chatGet = postSnapshot.getValue(Chat.class);
                    if(chatGet.mssv.equals(username)){
                        chat = postSnapshot.getValue(ChatOut.class);
                    }else{
                        chat = postSnapshot.getValue(ChatIn.class);
                    }
                    mMessageAdapter.chats.add(chat);
                }
                mMessageAdapter.notifyDataSetChanged();
                if(firstLoad){
                    mMessageRecycler.scrollToPosition((int)mMessageAdapter.chats.size() - 1);
                    firstLoad = !firstLoad;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        chatReference.addValueEventListener(valueChatEventListener);

        adminReference = mDatabase.child("Admin");
        valueAdminEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isAdmin = false;
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()){
                    admin = postSnapshot.getValue(Admin.class);
                    if(admin.mssv.equals(username)){
                        isAdmin = true;
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        adminReference.addValueEventListener(valueAdminEventListener);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        chatReference.removeEventListener(valueChatEventListener);
        adminReference.removeEventListener(valueAdminEventListener);
    }
}
