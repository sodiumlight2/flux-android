package org.nikanikoo.flux.ui.fragments.messages;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.nikanikoo.flux.data.models.Conversation;
import org.nikanikoo.flux.ui.adapters.messages.ConversationsAdapter;
import org.nikanikoo.flux.data.managers.MessagesManager;
import org.nikanikoo.flux.R;
import org.nikanikoo.flux.ui.activities.ChatActivity;
import org.nikanikoo.flux.ui.activities.MainActivity;
import org.nikanikoo.flux.ui.fragments.BaseFragment;
import org.nikanikoo.flux.ui.fragments.profile.ProfileFragment;
import org.nikanikoo.flux.ui.fragments.profile.GroupProfileFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class MessagesListFragment extends BaseFragment implements ConversationsAdapter.OnConversationClickListener {
    
    private RecyclerView recyclerView;
    private ConversationsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessagesManager messagesManager;
    private List<Conversation> conversations;
    private boolean isConversationsLoaded = false;
    private boolean isViewCreated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализируем список и менеджер один раз при создании фрагмента
        if (conversations == null) {
            conversations = new ArrayList<>();
        }
        if (messagesManager == null) {
            messagesManager = MessagesManager.getInstance(requireContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages_list, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupToolbarTitle();
        setupErrorView(view, R.id.swipe_refresh);
        setRetryCallback(() -> refreshConversations());

        isViewCreated = true;
        
        // Загружаем диалоги
        if (!isConversationsLoaded || conversations.isEmpty()) {
            refreshConversations();
        } else {
            // Если список уже загружен, просто обновляем адаптер
            adapter.notifyDataSetChanged();
        }
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Восстанавливаем заголовок при возврате
        setupToolbarTitle();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }
    
    private void setupToolbarTitle() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setToolbarTitle(getString(R.string.messages_title));
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        
        swipeRefreshLayout.setOnRefreshListener(this::refreshConversations);
    }

    private void setupRecyclerView() {
        adapter = new ConversationsAdapter(requireContext(), conversations);
        adapter.setOnConversationClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Обновить список диалогов
     */
    public void refreshConversations() {
        if (!isViewCreated) {
            return;
        }
        
        swipeRefreshLayout.setRefreshing(true);
        
        messagesManager.getConversations(20, 0, new MessagesManager.ConversationsCallback() {
            @Override
            public void onSuccess(List<Conversation> loadedConversations) {
                if (getActivity() != null && isViewCreated) {
                    getActivity().runOnUiThread(() -> {
                        hideError();
                        conversations.clear();
                        
                        org.nikanikoo.flux.data.managers.ProfileManager profileManager = 
                            org.nikanikoo.flux.data.managers.ProfileManager.getInstance(requireContext());
                        org.nikanikoo.flux.data.models.UserProfile myProfile = profileManager.getCachedProfileSync();
                        
                        Conversation selfConv = null;
                        if (myProfile != null) {
                            for (Conversation c : loadedConversations) {
                                if (c.getPeerId() == myProfile.getId()) {
                                    selfConv = c;
                                    break;
                                }
                            }
                            
                            if (selfConv != null) {
                                loadedConversations.remove(selfConv);
                                Conversation updatedSelf = new Conversation(
                                    selfConv.getId(),
                                    selfConv.getPeerId(),
                                    getString(R.string.chat_favorites),
                                    selfConv.getLastMessage() == null || selfConv.getLastMessage().isEmpty() ? getString(R.string.chat_favorites_subtitle) : selfConv.getLastMessage(),
                                    selfConv.getLastMessageDate(),
                                    selfConv.getUnreadCount()
                                );
                                updatedSelf.setPeerPhoto(myProfile.getPhoto200());
                                updatedSelf.setOnline(true);
                                updatedSelf.setPeerVerified(myProfile.isVerified());
                                selfConv = updatedSelf;
                            } else {
                                selfConv = new Conversation(
                                    myProfile.getId(),
                                    myProfile.getId(),
                                    getString(R.string.chat_favorites),
                                    getString(R.string.chat_favorites_subtitle),
                                    0,
                                    0
                                );
                                selfConv.setPeerPhoto(myProfile.getPhoto200());
                                selfConv.setOnline(true);
                                selfConv.setPeerVerified(myProfile.isVerified());
                            }
                        }
                        
                        if (selfConv != null) {
                            conversations.add(selfConv);
                        }
                        conversations.addAll(loadedConversations);
                        
                        if (adapter != null) {
                            adapter.updateConversations(conversations);
                        }
                        isConversationsLoaded = true;
                        swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isViewCreated) {
                    getActivity().runOnUiThread(() -> {
                        showErrorAuto(error);
                        swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }
 
    @Override
    public void onConversationClick(Conversation conversation) {
        // Открываем чат с выбранным пользователем
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_PEER_ID, conversation.getPeerId());
        intent.putExtra(ChatActivity.EXTRA_PEER_NAME, conversation.getTitle());
        intent.putExtra(ChatActivity.EXTRA_FROM_ID, conversation.getPeerId());
        startActivity(intent);
    }
    
    @Override
    public void onAvatarClick(int userId, String userName) {
        // Переход в профиль пользователя или группы
        if (userId > 0) {
            // Пользователь
            ProfileFragment profileFragment = ProfileFragment.newInstanceWithId(userId, userName);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack("profile_" + userId)
                    .commit();
        } else if (userId < 0) {
            // Группа (отрицательный ID)
            GroupProfileFragment groupProfileFragment = GroupProfileFragment.newInstance(-userId, userName);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, groupProfileFragment)
                    .addToBackStack("group_" + (-userId))
                    .commit();
        }
    }
    
    public void updateUserOnlineStatus(int userId, boolean isOnline) {
        if (getActivity() != null && isViewCreated) {
            getActivity().runOnUiThread(() -> {
                // Обновляем статус пользователя в адаптере
                if (adapter != null) {
                    adapter.updateUserOnlineStatus(userId, isOnline);
                }
            });
        }
    }
}