package com.example.chatapp.requests;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {

    private RequestAdapter adapter;
    private List<RequestModel> requestModelList;
    private TextView empty;

    private DatabaseReference databaseReferenceUsers;
    private View progressBar;

    public RequestsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvRequest = view.findViewById(R.id.rvRequests);
        empty = view.findViewById(R.id.emptyrequest);
        progressBar = view.findViewById(R.id.progressBar);

        rvRequest.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestModelList = new ArrayList<>();
        adapter = new RequestAdapter(getActivity(), requestModelList);
        rvRequest.setAdapter(adapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        DatabaseReference databaseReferenceRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        empty.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        databaseReferenceRequests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                requestModelList.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (ds.exists()) {
                        String requestType = ds.child(NodeNames.REQUEST_TYPE).getValue().toString();
                        if (requestType.equals(Constants.REQUEST_STATUS_RECEIVED)) {
                            final String userId = ds.getKey();
                            databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String userName = dataSnapshot.child(NodeNames.NAME).getValue().toString();
                                    String photoName = "";
                                    if (dataSnapshot.child(NodeNames.PHOTO).getValue() != null) {
                                        photoName = dataSnapshot.child(NodeNames.PHOTO).getValue().toString();
                                    }

                                    RequestModel requestModel = new RequestModel(userId, userName, photoName);
                                    requestModelList.add(requestModel);
                                    adapter.notifyDataSetChanged();
                                    empty.setVisibility(View.GONE);

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}