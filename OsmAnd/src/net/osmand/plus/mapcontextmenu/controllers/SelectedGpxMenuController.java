package net.osmand.plus.mapcontextmenu.controllers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.SelectedGpxMenuBuilder;
import net.osmand.plus.myplaces.SaveCurrentTrackTask;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SelectedGpxMenuController extends MenuController {

	private SelectedGpxPoint selectedGpxPoint;

	public SelectedGpxMenuController(@NonNull final MapActivity mapActivity, @NonNull PointDescription pointDescription,
	                                 @NonNull final SelectedGpxPoint selectedGpxPoint) {
		super(new SelectedGpxMenuBuilder(mapActivity, selectedGpxPoint), pointDescription, mapActivity);
		this.selectedGpxPoint = selectedGpxPoint;
		builder.setShowOnlinePhotos(false);

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				Intent intent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getTrackActivity());
				SelectedGpxFile selectedGpxFile = selectedGpxPoint.getSelectedGpxFile();
				if (selectedGpxFile.isShowCurrentTrack()) {
					intent.putExtra(TrackActivity.CURRENT_RECORDING, true);
				} else {
					intent.putExtra(TrackActivity.TRACK_FILE_NAME, selectedGpxFile.getGpxFile().path);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapActivity.startActivity(intent);
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);
		leftTitleButtonController.startIconId = R.drawable.ic_action_folder;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				new OpenGpxDetailsTask(selectedGpxPoint.getSelectedGpxFile(), selectedGpxPoint.getSelectedPoint(), mapActivity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.analyze_on_map);
		rightTitleButtonController.startIconId = R.drawable.ic_action_analyze_intervals;
	}

	private static class OpenGpxDetailsTask extends AsyncTask<Void, Void, GpxSelectionHelper.GpxDisplayItem> {

		private OsmandApplication app;

		private WptPt selectedPoint;
		private SelectedGpxFile selectedGpxFile;

		private ProgressDialog progressDialog;
		private WeakReference<MapActivity> activityRef;

		OpenGpxDetailsTask(SelectedGpxFile selectedGpxFile, WptPt selectedPoint, MapActivity mapActivity) {
			app = mapActivity.getMyApplication();
			this.activityRef = new WeakReference<>(mapActivity);
			this.selectedGpxFile = selectedGpxFile;
			this.selectedPoint = selectedPoint;
		}

		@Override
		protected void onPreExecute() {
			MapActivity activity = activityRef.get();
			if (activity != null && AndroidUtils.isActivityNotDestroyed(activity)) {
				if (selectedGpxFile.getGpxFile().path != null) {
					progressDialog = new ProgressDialog(activity);
					progressDialog.setTitle("");
					progressDialog.setMessage(app.getString(R.string.loading_data));
					progressDialog.setCancelable(false);
					progressDialog.show();
				}
			}
		}

		@Override
		protected GpxSelectionHelper.GpxDisplayItem doInBackground(Void... voids) {
			GpxSelectionHelper.GpxDisplayGroup gpxDisplayGroup = null;
			GPXUtilities.GPXFile gpxFile = null;
			GPXUtilities.Track generalTrack = null;
			if (selectedGpxFile.getGpxFile().path != null) {
				gpxFile = GPXUtilities.loadGPXFile(new File(selectedGpxFile.getGpxFile().path));
			}
			if (gpxFile != null) {
				generalTrack = gpxFile.getGeneralTrack();
			}
			if (generalTrack != null) {
				gpxFile.addGeneralTrack();
				gpxDisplayGroup = app.getSelectedGpxHelper().buildGeneralGpxDisplayGroup(gpxFile, generalTrack);
			} else if (gpxFile != null && gpxFile.tracks.size() > 0) {
				gpxDisplayGroup = app.getSelectedGpxHelper().buildGeneralGpxDisplayGroup(gpxFile, gpxFile.tracks.get(0));
			}
			List<GpxSelectionHelper.GpxDisplayItem> items = null;
			if (gpxDisplayGroup != null) {
				items = gpxDisplayGroup.getModifiableList();
			}
			if (items != null && items.size() > 0) {
				return items.get(0);
			}
			return null;
		}

		@Override
		protected void onPostExecute(GpxSelectionHelper.GpxDisplayItem gpxItem) {
			MapActivity activity = activityRef.get();
			if (activity != null) {
				if (progressDialog != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					progressDialog.dismiss();
				}
				if (gpxItem != null && gpxItem.analysis != null) {
					ArrayList<GpxUiHelper.GPXDataSetType> list = new ArrayList<>();
					if (gpxItem.analysis.hasElevationData) {
						list.add(GpxUiHelper.GPXDataSetType.ALTITUDE);
					}
					if (gpxItem.analysis.hasSpeedData) {
						list.add(GpxUiHelper.GPXDataSetType.SPEED);
					} else if (gpxItem.analysis.hasElevationData) {
						list.add(GpxUiHelper.GPXDataSetType.SLOPE);
					}
					if (list.size() > 0) {
						gpxItem.chartTypes = list.toArray(new GpxUiHelper.GPXDataSetType[0]);
					}
					gpxItem.locationOnMap = selectedPoint;
					OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(gpxItem.locationStart.lat, gpxItem.locationStart.lon,
							settings.getLastKnownMapZoom(),
							new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
							false,
							gpxItem);
					activity.getContextMenu().hide();
					MapActivity.launchMapActivityMoveToTop(activity);
				}
			}
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof SelectedGpxPoint) {
			this.selectedGpxPoint = (SelectedGpxPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return selectedGpxPoint;
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_gpx_track);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public Drawable getRightIcon() {
		int color = isLight() ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		return getIcon(R.drawable.ic_action_polygom_dark, color);
	}

	@Override
	public void share(LatLon latLon, String title, String address) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && selectedGpxPoint != null) {
			final GPXFile gpxFile = selectedGpxPoint.getSelectedGpxFile().getGpxFile();
			if (gpxFile != null) {
				if (Algorithms.isEmpty(gpxFile.path)) {
					SaveGpxListener saveGpxListener = new SaveGpxListener() {
						@Override
						public void gpxSavingStarted() {

						}

						@Override
						public void gpxSavingFinished(Exception errorMessage) {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity != null) {
								shareGpx(mapActivity, gpxFile.path);
							}
						}
					};
					new SaveCurrentTrackTask(mapActivity.getMyApplication(), gpxFile, saveGpxListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					shareGpx(mapActivity, gpxFile.path);
				}
			}
		} else {
			super.share(latLon, title, "");
		}
	}

	private void shareGpx(@NonNull Context context, @NonNull String path) {
		final Uri fileUri = AndroidUtils.getUriForFile(context, new File(path));
		final Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		sendIntent.setType("application/gpx+xml");
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (AndroidUtils.isIntentSafe(context, sendIntent)) {
			context.startActivity(sendIntent);
		}
	}

	public static class SelectedGpxPoint {

		private final WptPt selectedPoint;
		private final SelectedGpxFile selectedGpxFile;
		private final float bearing;

		public SelectedGpxPoint(SelectedGpxFile selectedGpxFile, WptPt selectedPoint, float bearing) {
			this.selectedPoint = selectedPoint;
			this.selectedGpxFile = selectedGpxFile;
			this.bearing = bearing;
		}

		public WptPt getSelectedPoint() {
			return selectedPoint;
		}

		public SelectedGpxFile getSelectedGpxFile() {
			return selectedGpxFile;
		}

		public float getBearing() {
			return bearing;
		}
	}
}