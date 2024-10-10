package com.pangbai.dowork.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pangbai.dowork.Command.CommandBuilder;
import com.pangbai.dowork.Command.cmdExer;
import com.pangbai.dowork.PropertiesActivity;
import com.pangbai.dowork.R;
import com.pangbai.dowork.databinding.FragmentContainerBinding;
import com.pangbai.dowork.tool.IO;
import com.pangbai.dowork.tool.Init;
import com.pangbai.dowork.tool.containerInfor;
import com.pangbai.dowork.tool.ctAdapter;
import com.pangbai.dowork.tool.uiThreadUtil;
import com.pangbai.dowork.tool.util;
import com.pangbai.linuxdeploy.PrefStore;
import com.pangbai.view.dialogUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class containerFragment extends Fragment implements View.OnClickListener {


    ctAdapter.OnItemChange mOnItemChange = new ctAdapter.OnItemChange() {
        @Override
        public void OnItemChange(containerInfor infor) {
            binding.ctMethod.setText(infor.method.toUpperCase());
            containerInfor.ct = infor;
        }
    };
    FragmentContainerBinding binding;
    ctAdapter adapter;
    // containerInfor currentContainer;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // binding = FragmentContainerBinding.inflate(inflater);

        binding.ctAdd.setOnClickListener(this);
        binding.ctDelete.setOnClickListener(this);
        binding.ctRename.setOnClickListener(this);
        executorService = Executors.newSingleThreadExecutor();

        return binding.getRoot();
    }


    @Override
    public void onClick(View view) {
        if (view == binding.ctAdd) {
            dialogUtils.showInputDialog(getContext(),
                    "Create container",
                    userInput -> {
                        if (containerInfor.getContainerByName(userInput) != null) {
                            Toast.makeText(getActivity(), "Container already exists", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (userInput == null)
                            return;
                        PrefStore.changeProfile(getContext(), userInput);
                        util.startActivity(getActivity(), PropertiesActivity.class, false);
                    });

        } else if (view == binding.ctDelete) {
            int containerSize = containerInfor.ctList.size();
            if (containerSize == 1) {
                Toast.makeText(getContext(), "Please ensure there is at least one container", Toast.LENGTH_SHORT).show();
                return;
            }
            if (containerInfor.ct != null)
                dialogUtils.showConfirmationDialog(getContext(),
                        "Delete Container",
                        "Are you sure you want to delete " + containerInfor.ct.name + "? You will lose the container's data.",
                        "Confirm Deletion",
                        "Cancel",
                        () -> {
                            Dialog mdialog = dialogUtils.showCustomLayoutDialog(getContext(), "Deleting container" + containerInfor.ct.name, R.layout.dialog_loading);
                            new Thread() {
                                @Override
                                public void run() {
                                    if (!containerInfor.isProot(containerInfor.ct))
                                        CommandBuilder.stopChroot();
                                    boolean result = containerInfor.reMoveContainer(containerInfor.ct);
                                    mdialog.dismiss();
                                    if (!result)
                                        return;
                                    if (--ctAdapter.selectedPosition < 0)
                                        ctAdapter.selectedPosition = 0;
                                    containerInfor.ctList.remove(containerInfor.ct);
                                    containerInfor.ct = containerInfor.ctList.get(ctAdapter.selectedPosition);
                                    PrefStore.changeProfile(getContext(), containerInfor.ct.name);
                                    uiThreadUtil.runOnUiThread(() -> {
                                        adapter.notifyDataSetChanged();
                                    });
                                }
                            }.start();
                        },
                        null);

        } else if (view == binding.ctRename) {
            dialogUtils.showInputDialog(getContext(),
                    "Rename container",
                    userInput -> {
                        if (containerInfor.getContainerByName(userInput) != null) {
                            Toast.makeText(getActivity(), "Rename failed, container already exists", Toast.LENGTH_LONG).show();
                            return;
                        }
                        File oldFile = PrefStore.getPropertiesConfFile(getContext());
                        File newFile = new File(PrefStore.getEnvDir(getContext()) + "/config/" + userInput + ".conf");
                        oldFile.renameTo(newFile);
                        containerInfor.ct.name = userInput;
                        adapter.notifyDataSetChanged();
                        PrefStore.changeProfile(getContext(), userInput);
                    });


        }
    }




    @Override
    public void onResume() {
        super.onResume();
        // Toast.makeText(getActivity(), "resume", Toast.LENGTH_LONG).show();
        doInBackground();
    }


    public void doInBackground() {
        executorService.submit(() -> {
            List<String> ctName = containerInfor.getProfiles(getContext());
            List ctList = containerInfor.setInforList(ctName);

            uiThreadUtil.runOnUiThread(() -> {
                //  adapter.setData(null);
                adapter.setData(ctList);
                binding.progressBar.setVisibility(View.GONE);
            });

        });
    }


    @Override
    public void onDestroyView() {
        if (!executorService.isShutdown())
            executorService.shutdownNow();
     //   Toast.makeText(getActivity(), "destory", Toast.LENGTH_LONG).show();
        adapter.ItemChange = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        binding = FragmentContainerBinding.inflate(getLayoutInflater());
        adapter = new ctAdapter(containerInfor.ctList, mOnItemChange);
        binding.ctList.setAdapter(adapter);
        binding.ctList.setLayoutManager(new LinearLayoutManager(getContext()));
    }


}
