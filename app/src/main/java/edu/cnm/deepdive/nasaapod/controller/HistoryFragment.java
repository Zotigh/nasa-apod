/*
 *  Copyright 2019 Nicholas Bennett & Deep Dive Coding
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.nasaapod.controller;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import edu.cnm.deepdive.android.BaseFluentAsyncTask;
import edu.cnm.deepdive.nasaapod.R;
import edu.cnm.deepdive.nasaapod.model.entity.Access;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.model.pojo.ApodWithAccesses;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.DeleteApodTask;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.InsertAccessTask;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.SelectAllApodTask;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.SelectAllApodWithAccessesTask;
import edu.cnm.deepdive.nasaapod.service.FileStorageService;
import edu.cnm.deepdive.nasaapod.service.FragmentService;
import edu.cnm.deepdive.nasaapod.view.HistoryAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hosts a {@link RecyclerView} of {@link Apod} instances from the local database, allowing user
 * selection for display in the {@link ImageFragment} set by {@link #setImageFragment(ImageFragment)}.
 */
public class HistoryFragment extends Fragment implements View.OnClickListener {

  private List<ApodWithAccesses> history;
  private HistoryAdapter adapter;
  private ImageFragment imageFragment;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_history, container, false);
    RecyclerView historyView = view.findViewById(R.id.history_view);
    history = new ArrayList<>();
    adapter = new HistoryAdapter(this, history);
    historyView.setAdapter(adapter);
    refresh();
    return view;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    refresh();
  }

  /**
   * Handles a click on a {@link View} in the {@link RecyclerView} by extracting the {@link Apod}
   * reference returned by {@link View#getTag()}, showing the image fragment, invoking {@link
   * ImageFragment#setApod(Apod)}, and finally updating the {@link
   * android.support.design.widget.BottomNavigationView} in {@link NavActivity}.
   *
   * @param view visual presentation of a single {@link Apod} instance.
   */
  @Override
  public void onClick(View view) {
    Apod apod = (Apod) view.getTag();
    NavActivity activity = ((NavActivity) getActivity());
    FragmentService.getInstance().showFragment(activity, R.id.fragment_container, imageFragment);
    Access access = new Access();
    access.setApodId(apod.getId());
    new InsertAccessTask().execute(access);
    imageFragment.setApod(apod);
    activity.getNavigation().setSelectedItemId(R.id.navigation_image);
  }

  /**
   * Sets the {@link ImageFragment} used for APOD image display.
   *
   * @param fragment display host {@link ImageFragment}.
   */
  public void setImageFragment(ImageFragment fragment) {
    imageFragment = fragment;
  }

  /**
   * Queries the local database for {@link Apod} instances, populating (indirectly) a {@link
   * RecyclerView} with the results.
   */
  public void refresh() {
    if (!isHidden()) {
      new SelectAllApodWithAccessesTask()
          .setSuccessListener((apods) -> {
            history.clear();
            history.addAll(apods);
            adapter.notifyDataSetChanged();
          })
          .execute();
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    return super.onContextItemSelected(item);
  }

  /**
   * Creates a context menu for the specified {@link Apod} instance at the given
   * <code>position</code> in the recycler view. Depending on the APOD media content, the
   * <strong>Download</strong> menu item may be disabled &amp; hidden.
   *
   * @param menu context menu to which items will be added.
   * @param position position in recycler view of context {@link Apod} instance.
   * @param apod context {@link Apod} instance.
   */
  public void createContextMenu(ContextMenu menu, int position, Apod apod) {
    FileStorageService service = FileStorageService.getInstance();
    getActivity().getMenuInflater().inflate(R.menu.item_context, menu);
    menu.findItem(R.id.context_delete).setOnMenuItemClickListener((item) -> {
      deleteApod(apod, position);
      return true;
    });
    MenuItem download = menu.findItem(R.id.context_download);
    if (apod.isMediaImage()) {
      download.setOnMenuItemClickListener((item) -> {
        ((NavActivity) getActivity()).downloadApod(apod);
        return true;
      });
    } else {
      download.setEnabled(false).setVisible(false);
    }
    menu.findItem(R.id.context_info).setOnMenuItemClickListener((item) -> {
      ((NavActivity) getActivity()).showFullInfo(apod);
      return true;
    });
  }

  private void deleteApod(Apod apod, int position) {
    FileStorageService service = FileStorageService.getInstance();
    new DeleteApodTask()
        .setTransformer((v) -> {
          service.deleteInternalFile(getContext(), service.filenameFromUrl(apod.getUrl()));
          return null;
        })
        .setSuccessListener((v) -> {
          history.remove(position);
          adapter.notifyItemRemoved(position);
          if (apod.equals(imageFragment.getApod())) {
            imageFragment.setApod(null);
          }
        })
        .execute(apod);
  }

}
