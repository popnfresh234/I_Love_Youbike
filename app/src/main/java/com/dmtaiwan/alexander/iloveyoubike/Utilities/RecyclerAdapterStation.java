package com.dmtaiwan.alexander.iloveyoubike.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dmtaiwan.alexander.iloveyoubike.R;
import com.dmtaiwan.alexander.iloveyoubike.Data.StationContract;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Alexander on 7/27/2015.
 */
public class RecyclerAdapterStation extends RecyclerView.Adapter<RecyclerAdapterStation.ViewHolder> {

    private static final String LOG_TAG = RecyclerAdapterStation.class.getSimpleName();


    private Cursor mCursor;
    final private Context mContext;
    final private View mEmptyView;


    public RecyclerAdapterStation(Context context, View emptyView) {
        mContext = context;
        mEmptyView = emptyView;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public static interface StationAdapterOnClickHandler {
        void onClick(int stationId, RecyclerAdapterStation.ViewHolder vh);
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_item_station_list, parent, false);
        return new ViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {



        mCursor.moveToPosition(position);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String language = preferences.getString(mContext.getString(R.string.pref_key_language), mContext.getString(R.string.pref_language_english));
        if (language.equals(mContext.getString(R.string.pref_language_english))) {
            holder.stationName.setText(mCursor.getString(StationContract.COL_STATION_NAME_EN));
        }else if (language.equals(mContext.getString(R.string.pref_language_pinyin))){
            //Look up the station's pinyin string based on station id
            int stringId = mContext.getResources().getIdentifier("station" + String.valueOf(mCursor.getInt(StationContract.COL_STATION_ID)), "string", mContext.getPackageName());
            String stationName = mContext.getString(stringId);
            holder.stationName.setText(stationName);
        }
        else{
            holder.stationName.setText(mCursor.getString(StationContract.COL_STATION_NAME_ZH));
        }

        //Set transition names so views can be found if activity is recreated ie after rotation
        ViewCompat.setTransitionName(holder.stationName, mContext.getResources().getString(R.string.transition_station_name_text) + position);
        ViewCompat.setTransitionName(holder.stationStatus, mContext.getResources().getString(R.string.transition_status_image_view) + position);

        String time = Utilities.formatTime(mCursor.getString(StationContract.COL_LAST_UPDATED));
        holder.time.setText(time);

        //calculate the distance from the user's last known location
        double stationLat = mCursor.getDouble(StationContract.COL_STATION_LAT);
        double stationLong = mCursor.getDouble(StationContract.COL_STATION_LONG);

        Location userLocation = Utilities.getUserLocation(mContext);

        if (userLocation != null) {
            float distance = Utilities.calculateDistance(stationLat, stationLong, userLocation);
            holder.distance.setText(Utilities.formatDistance(distance));
        }

        //Set the status icon
        holder.stationStatus.setImageResource(Utilities.getStatusIconDrawable(mCursor, Utilities.ICON_SIZE_SMALL));

        //set the content description
        holder.stationStatus.setContentDescription(Utilities.getContentDescription(mCursor, mContext));
    }




    @Override
    public int getItemCount() {
        if ( null == mCursor ) return 0;
        return mCursor.getCount();

    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        @Bind(R.id.image_view_station_list_status)
        public ImageView stationStatus;
        @Bind(R.id.text_view_station_list_station_name)
        public TextView stationName;
        @Bind(R.id.text_view_station_list_time)
        TextView time;
        @Bind(R.id.text_view_station_list_distance) TextView distance;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int stationId = mCursor.getInt(StationContract.COL_STATION_ID);
            RecyclerEvent recyclerEvent = new RecyclerEvent();
            recyclerEvent.setStationId(stationId);
            recyclerEvent.setVh(this);
            EventBus.getInstance().post(recyclerEvent);
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void swapCursor(Cursor newCursor) {
        //Update the user locatoin
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }


}
