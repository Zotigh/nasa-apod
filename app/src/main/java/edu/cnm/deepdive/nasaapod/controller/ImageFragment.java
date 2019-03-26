package edu.cnm.deepdive.nasaapod.controller;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import edu.cnm.deepdive.nasaapod.R;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.DeleteApodTask;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.InsertApodTask;
import edu.cnm.deepdive.nasaapod.service.ApodDBService.SelectApodTask;
import edu.cnm.deepdive.nasaapod.service.ApodWebService.GetFromNasaTask;
import edu.cnm.deepdive.nasaapod.service.FileStorageService;
import edu.cnm.deepdive.util.Date;
import java.io.IOException;
import java.util.Calendar;

/**
 * Populates a {@link WebView} with the image or video URL of the APOD for the currently rselected
 * date. If the {@link Apod} instance for the selected date is not in the local database, a request
 * is made to retrieve it from the NASA APOD web service.
 */
public class ImageFragment extends Fragment {

  private static final String APOD_KEY = "apod";

  private WebView webView;
  private Apod apod;
  private HistoryFragment historyFragment;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    setRetainInstance(true);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_image, container, false);
    setupWebView(view);
    if (savedInstanceState != null) {
      apod = (Apod) savedInstanceState.getSerializable(APOD_KEY);
    }
    setApod(apod);
    return view;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.image_options, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.image_download).setVisible(apod != null && apod.isMediaImage());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean handled = true;
    switch(item.getItemId()) {
      case R.id.image_info:
        getNavActivity().showFullInfo(apod);
        break;
      case R.id.image_download:
        getNavActivity().downloadApod(apod);
        break;
      case R.id.image_delete:
        deleteApod(apod);
        break;
      default:
        handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(APOD_KEY, apod);
  }

  /**
   * Returns the currently displayed {@link Apod} instance.
   *
   * @return current {@link Apod} instance.
   */
  public Apod getApod() {
    return apod;
  }

  /**
   * Sets the {@link Apod} instance to be displayed.
   *
   * @param apod current {@link Apod} instance.
   */
  public void setApod(Apod apod) {
    if (apod != null) {
      FileStorageService service = FileStorageService.getInstance();
      this.apod = apod;
      getNavActivity().getLoading().setVisibility(View.VISIBLE);
      String url = apod.getUrl();
      if (apod.isMediaImage() && service.internalFileExists(service.filenameFromUrl(url))) {
        url = service.internalUrlFromFilename(service.filenameFromUrl(url));
      }
      webView.loadUrl(url);
      getActivity().invalidateOptionsMenu();
    } else {
      loadApod(Date.fromCalendar(Calendar.getInstance()));
    }

  }

  /**
   * Sets the {@link HistoryFragment} to be refreshed on successful retrieval of an {@link Apod}
   * instance from the NASA APOD web service.
   *
   * @param historyFragment host {@link HistoryFragment} for list of {@link Apod} instances in local
   * database.
   */
  public void setHistoryFragment(HistoryFragment historyFragment) {
    this.historyFragment = historyFragment;
  }

  /**
   * Loads {@link Apod} instance for specified {@link Date} from local database, or&mdash;if the
   * {@link Apod} for the specified date is not stored locally&mdash;requests it from the NASA APOD
   * web service.
   *
   * @param date desired {@link Apod} date.
   */
  public void loadApod(Date date) {
    getNavActivity().getLoading().setVisibility(View.VISIBLE);
    new SelectApodTask()
        .setTransformer((apod) -> {
          saveIfNeeded(apod);
          return apod;
        })
        .setSuccessListener(this::setApod)
        .setFailureListener((nullApod) -> {
          new GetFromNasaTask()
              .setTransformer((apod) -> {
                new InsertApodTask(isVisible()).execute(apod);
                saveIfNeeded(apod);
                return apod;
              })
              .setSuccessListener((apod) -> {
                historyFragment.refresh();
                setApod(apod);
              })
              .setFailureListener((anotherNullApod) -> showFailure())
              .execute(date);
        })
        .execute(date);
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void setupWebView(View view) {
    webView = view.findViewById(R.id.web_view);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        getNavActivity().getLoading().setVisibility(View.GONE);
        if (isVisible()) {
          showInfo();
        }
      }
    });
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setSupportZoom(true);
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setUseWideViewPort(true);
    settings.setLoadWithOverviewMode(true);
  }

  private NavActivity getNavActivity() {
    return (NavActivity) getActivity();
  }

  private void saveIfNeeded(Apod apod) {
    FileStorageService service = FileStorageService.getInstance();
    String filename = service.filenameFromUrl(apod.getUrl());
    if (apod.isMediaImage() && !service.internalFileExists(filename)) {
      service.downloadToFile(apod.getUrl(), true);
    }
  }

  private void showInfo() {
    if (apod != null && isVisible()) {
      Toast.makeText(getContext(), apod.getTitle(), Toast.LENGTH_LONG).show();
    }
  }

  private void showFailure() {
    getNavActivity().getLoading().setVisibility(View.GONE);
    Toast.makeText(getContext(), R.string.error_message, Toast.LENGTH_LONG).show();
  }

  private void deleteApod(Apod apod) {
    FileStorageService service = FileStorageService.getInstance();
    new DeleteApodTask()
        .setTransformer((v) -> {
          service.deleteInternalFile(service.filenameFromUrl(apod.getUrl()));
          return null;
        })
        .setSuccessListener((v) -> {
          setApod(null);
        })
        .execute(apod);
  }

}
