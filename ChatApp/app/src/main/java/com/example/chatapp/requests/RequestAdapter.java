package com.example.chatapp.requests;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;


public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<RequestModel> requestModelList;
    private DatabaseReference databaseReferenceFriendRequests, databaseReferenceChats;
    private FirebaseUser currentUser;

    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_layout, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RequestViewHolder holder, int position) {

        final RequestModel requestModel = requestModelList.get(position);
        holder.tvFullName.setText(requestModel.getUserName());
        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER + "/" + requestModel.getPhotoName());
        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.loadingcircle)
                        .error(R.drawable.user)
                        .into(holder.imageView);
            }
        });

        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        holder.btnaccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.pbDecision.setVisibility(View.VISIBLE);
                holder.btndeny.setVisibility(View.GONE);
                holder.btnaccept.setVisibility(View.GONE);

                final String userId = requestModel.getUserId();
                databaseReferenceChats.child(currentUser.getUid()).child(userId)
                        .child(NodeNames.TIME_STAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            databaseReferenceChats.child(userId).child(currentUser.getUid())
                                    .child(NodeNames.TIME_STAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        databaseReferenceFriendRequests.child(currentUser.getUid()).child(userId)
                                                .child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    databaseReferenceFriendRequests.child(userId).child(currentUser.getUid())
                                                            .child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(context, "Friend Request Accepted Successfully", Toast.LENGTH_SHORT).show();
                                                                holder.pbDecision.setVisibility(View.GONE);
                                                                holder.btndeny.setVisibility(View.VISIBLE);
                                                                holder.btnaccept.setVisibility(View.VISIBLE);

                                                            } else {
                                                                handleException(holder, task.getException());
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    handleException(holder, task.getException());
                                                }
                                            }
                                        });

                                    } else {
                                        handleException(holder, task.getException());
                                    }
                                }
                            });


                        } else {
                            handleException(holder, task.getException());

                        }

                    }
                });


            }
        });


        holder.btndeny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.pbDecision.setVisibility(View.VISIBLE);
                holder.btndeny.setVisibility(View.GONE);
                holder.btnaccept.setVisibility(View.GONE);

                final String userId = requestModel.getUserId();
                databaseReferenceFriendRequests.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            databaseReferenceFriendRequests.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(context, "Friend Request Denied Successfully", Toast.LENGTH_SHORT).show();
                                        holder.pbDecision.setVisibility(View.GONE);
                                        holder.btnaccept.setVisibility(View.VISIBLE);
                                        holder.btndeny.setVisibility(View.VISIBLE);
                                    } else {
                                        Toast.makeText(context, "Failed To Deny Friend Request", Toast.LENGTH_SHORT).show();
                                        holder.pbDecision.setVisibility(View.GONE);
                                        holder.btnaccept.setVisibility(View.VISIBLE);
                                        holder.btndeny.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(context, "Failed To Deny Friend Request", Toast.LENGTH_SHORT).show();
                            holder.pbDecision.setVisibility(View.GONE);
                            holder.btnaccept.setVisibility(View.VISIBLE);
                            holder.btndeny.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }
        });


    }

    private void handleException(RequestViewHolder holder, Exception exception) {
        Toast.makeText(context, "Failed To Accept Friend Request", Toast.LENGTH_SHORT).show();
        holder.pbDecision.setVisibility(View.GONE);
        holder.btndeny.setVisibility(View.VISIBLE);
        holder.btnaccept.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    public class RequestViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvFullName;
        private final ImageView imageView;
        private final Button btnaccept;
        private final Button btndeny;
        private final ProgressBar pbDecision;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            tvFullName = itemView.findViewById(R.id.tvFullName);
            imageView = itemView.findViewById(R.id.imageview);
            btnaccept = itemView.findViewById(R.id.btnaccept);
            btndeny = itemView.findViewById(R.id.btndeny);
            pbDecision = itemView.findViewById(R.id.pbDecision);
        }
    }
}
