package me.calebjones.spacelaunchnow.content.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.content.database.SharedPreference;
import me.calebjones.spacelaunchnow.content.models.GridItem;
import me.calebjones.spacelaunchnow.ui.activity.OrbiterDetailActivity;
import me.calebjones.spacelaunchnow.ui.activity.VehicleDetailActivity;
import me.calebjones.spacelaunchnow.utils.OnItemClickListener;
import me.calebjones.spacelaunchnow.utils.Utils;
import timber.log.Timber;

/**
 * This adapter takes data from SharedPreference/LoaderService and applies it to the LaunchesFragment
 */
public class OrbiterAdapter extends RecyclerView.Adapter<OrbiterAdapter.ViewHolder>{

    public int position;
    private Context mContext;
    private Calendar rightNow;
    private SharedPreferences sharedPref;
    private List<GridItem> items = new ArrayList<GridItem>();
    private static SharedPreference sharedPreference;
    private OnItemClickListener onItemClickListener;
    private int defaultBackgroundcolor;
    private static final int SCALE_DELAY = 30;
    private int lastPosition = -1;

    public OrbiterAdapter(Context context) {
        rightNow = Calendar.getInstance();
        items = new ArrayList();
        sharedPreference = SharedPreference.getInstance(context);
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        this.mContext = context;
    }

    public void addItems(List<GridItem> items) {
        if (this.items == null) {
            this.items = items;
        } else {
            this.items.addAll(items);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        int m_theme;

        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        sharedPreference = SharedPreference.getInstance(mContext);

        if (sharedPreference.getNightMode()) {
            m_theme = R.layout.gridview_item;
            defaultBackgroundcolor = ContextCompat.getColor(mContext, R.color.colorAccent);

        } else {
            m_theme = R.layout.gridview_item;
            defaultBackgroundcolor = ContextCompat.getColor(mContext, R.color.darkAccent);
        }
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(m_theme, viewGroup, false);
        return new ViewHolder(v, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int i) {
        final GridItem item = items.get(i);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Glide.with(mContext)
                    .load(item.getDrawableId())
                    .asBitmap()
                    .into(new BitmapImageViewTarget(holder.picture) {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            setCellColors(bitmap, holder, position);
                            amimateCell(holder);
                        }
                    });
        } else {
            Glide.with(mContext)
                    .load(item.getDrawableId())
                    .into(holder.picture);
        }

        holder.name.setText(item.getName());
    }

    private void amimateCell(ViewHolder holder) {

        int cellPosition = holder.getPosition();

        if (!holder.animated) {

            holder.animated = true;
            holder.grid_root.setScaleY(0);
            holder.grid_root.setScaleX(0);
            holder.grid_root.animate()
                    .scaleY(1).scaleX(1)
                    .setDuration(200)
                    .setStartDelay(SCALE_DELAY * cellPosition)
                    .start();
        }

    }

    public void setCellColors(Bitmap b, final ViewHolder viewHolder, final int position) {

        if (b != null) {
            Palette.generateAsync(b, new Palette.PaletteAsyncListener() {

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onGenerated(Palette palette) {

                    Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();

                    if (vibrantSwatch != null) {

                        viewHolder.name.setTextColor(vibrantSwatch.getTitleTextColor());
                        viewHolder.picture.setTransitionName("cover" + position);

                        Utils.animateViewColor(viewHolder.grid_root, defaultBackgroundcolor,
                                vibrantSwatch.getRgb());

                    } else {

                        Log.e("[ERROR]", "BookAdapter onGenerated - The VibrantSwatch were null at: " + position);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View grid_root;
        public ImageView picture;
        public TextView name;
        private OnItemClickListener onItemClickListener;
        protected boolean animated = false;

        //Add content to the card
        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);

            this.onItemClickListener = onItemClickListener;
            grid_root = view.findViewById(R.id.grid_root);
            picture = (ImageView) view.findViewById(R.id.picture);
            name = (TextView) view.findViewById(R.id.text);
            grid_root.setOnClickListener(this);
        }

        //React to click events.
        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            Timber.d("%s clicked.", position);
            onItemClickListener.onClick(v, getAdapterPosition());
        }
    }
}
