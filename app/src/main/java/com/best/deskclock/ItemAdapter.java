/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static androidx.recyclerview.widget.RecyclerView.NO_ID;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Base adapter class for displaying a collection of items. Provides functionality for handling
 * changing items, persistent item state, item click events, and re-usable item views.
 */
public class ItemAdapter<T extends ItemAdapter.ItemHolder<?>>
        extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder<?>> {

    /**
     * Factories for creating new {@link ItemViewHolder} entities.
     */
    private final SparseArray<ItemViewHolder.Factory> mFactoriesByViewType = new SparseArray<>();

    /**
     * Listeners to invoke in {@link #mOnItemClickedListener}.
     */
    private final SparseArray<OnItemClickedListener> mListenersByViewType = new SparseArray<>();

    /**
     * Listeners to invoke in {@link #mOnItemLongClickedListener}.
     */
    private final SparseArray<OnItemLongClickedListener> mListenersOnLongClickByViewType = new SparseArray<>();

    /**
     * Invokes the {@link OnItemClickedListener} in {@link #mListenersByViewType} corresponding
     * to {@link ItemViewHolder#getItemViewType()}
     */
    private final OnItemClickedListener mOnItemClickedListener = (viewHolder, id) -> {
        final OnItemClickedListener listener =
                mListenersByViewType.get(viewHolder.getItemViewType());
        if (listener != null) {
            listener.onItemClicked(viewHolder, id);
        }
    };

    /**
     * Invokes the {@link OnItemLongClickedListener} in {@link #mListenersOnLongClickByViewType}
     * corresponding to {@link ItemViewHolder#getItemViewType()}
     */
    private final OnItemLongClickedListener mOnItemLongClickedListener = (viewHolder, id) -> {
        final OnItemLongClickedListener listener =
                mListenersOnLongClickByViewType.get(viewHolder.getItemViewType());
        if (listener != null) {
            listener.onItemLongClicked(viewHolder, id);
        }
    };

    /**
     * Invoked when any item changes.
     */
    private OnItemChangedListener mOnItemChangedListener;

    /**
     * List of current item holders represented by this adapter.
     */
    private List<T> mItemHolders;

    /**
     * Finds the position of the changed item holder and invokes {@link #notifyItemChanged(int)} or
     * {@link #notifyItemChanged(int, Object)} if payloads are present (in order to do in-place
     * change animations).
     */
    private final OnItemChangedListener mItemChangedNotifier = new OnItemChangedListener() {
        @Override
        public void onItemChanged(ItemHolder<?> itemHolder) {
            if (mOnItemChangedListener != null) {
                mOnItemChangedListener.onItemChanged(itemHolder);
            }
            final int position = mItemHolders.indexOf(itemHolder);
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }
    };

    /**
     * Convenience for calling {@link #setHasStableIds(boolean)} with {@code true}.
     */
    public void setHasStableIds() {
        setHasStableIds(true);
    }

    /**
     * Sets the {@link ItemViewHolder.Factory} and {@link OnItemClickedListener} used to create
     * new item view holders in {@link #onCreateViewHolder(ViewGroup, int)}.
     *
     * @param factory   the {@link ItemViewHolder.Factory} used to create new item view holders
     * @param listener  the {@link OnItemClickedListener} to be invoked by
     *                  {@link #mItemChangedNotifier}
     * @param onLongClickedListener the {@link OnItemLongClickedListener} to be invoked by
     *                              {@link #mItemChangedNotifier}
     * @param viewTypes the unique identifier for the view types to be created
     * @return this object, allowing calls to methods in this class to be chained
     */
    public ItemAdapter<T> withViewTypes(ItemViewHolder.Factory factory,
                                        OnItemClickedListener listener,
                                        OnItemLongClickedListener onLongClickedListener,
                                        int... viewTypes) {

        for (int viewType : viewTypes) {
            mFactoriesByViewType.put(viewType, factory);
            mListenersByViewType.put(viewType, listener);
            mListenersOnLongClickByViewType.put(viewType, onLongClickedListener);
        }

        return this;
    }

    /**
     * @return the current list of item holders represented by this adapter
     */
    public final List<T> getItems() {
        return mItemHolders;
    }

    /**
     * Sets the list of item holders to serve as the dataset for this adapter and invokes
     * {@link #notifyDataSetChanged()} to update the UI.
     * <p/>
     * If {@link #hasStableIds()} returns {@code true}, then the instance state will preserved
     * between new and old holders that have matching {@link ItemHolder#itemId} values.
     *
     * @param itemHolders the new list of item holders
     */
    public void setItems(List<T> itemHolders) {
        final List<T> oldItemHolders = mItemHolders;
        if (oldItemHolders != itemHolders) {
            if (oldItemHolders != null) {
                // remove the item change listener from the old item holders
                for (T oldItemHolder : oldItemHolders) {
                    oldItemHolder.removeOnItemChangedListener(mItemChangedNotifier);
                }
            }

            if (oldItemHolders != null && itemHolders != null && hasStableIds()) {
                // transfer instance state from old to new item holders based on item id,
                // we use a simple O(N^2) implementation since we assume the number of items is
                // relatively small and generating a temporary map would be more expensive
                final Bundle bundle = new Bundle();
                for (T newItemHolder : itemHolders) {
                    for (T oldItemHolder : oldItemHolders) {
                        if (newItemHolder.itemId == oldItemHolder.itemId
                                && newItemHolder != oldItemHolder) {
                            // clear any existing state from the bundle
                            bundle.clear();

                            // transfer instance state from old to new item holder
                            oldItemHolder.onSaveInstanceState(bundle);
                            newItemHolder.onRestoreInstanceState(bundle);

                            break;
                        }
                    }
                }
            }

            if (itemHolders != null) {
                // add the item change listener to the new item holders
                for (T newItemHolder : itemHolders) {
                    newItemHolder.addOnItemChangedListener(mItemChangedNotifier);
                }
            }

            // finally update the current list of item holders and inform the RV to update the UI
            mItemHolders = itemHolders;
            notifyDataSetChanged();
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present
     * (optional operation). If this list does not contain the element, it is unchanged. Invokes
     * {@link #notifyItemRemoved} to update the UI.
     *
     * @param itemHolder the item holder to remove
     */
    public void removeItem(@NonNull T itemHolder) {
        final int index = mItemHolders.indexOf(itemHolder);
        if (index >= 0) {
            itemHolder = mItemHolders.remove(index);
            itemHolder.removeOnItemChangedListener(mItemChangedNotifier);
            notifyItemRemoved(index);
        }
    }

    /**
     * Sets the listener to be invoked whenever any item changes.
     */
    public void setOnItemChangedListener(OnItemChangedListener listener) {
        mOnItemChangedListener = listener;
    }

    @Override
    public int getItemCount() {
        return mItemHolders == null ? 0 : mItemHolders.size();
    }

    @Override
    public long getItemId(int position) {
        return hasStableIds() ? mItemHolders.get(position).itemId : NO_ID;
    }

    public T findItemById(long id) {
        for (T holder : mItemHolders) {
            if (holder.itemId == id) {
                return holder;
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return mItemHolders.get(position).getItemViewType();
    }

    @NonNull
    @Override
    public ItemViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final ItemViewHolder.Factory factory = mFactoriesByViewType.get(viewType);
        if (factory != null) {
            return factory.createViewHolder(parent, viewType);
        }
        throw new IllegalArgumentException("Unsupported view type: " + viewType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(ItemViewHolder viewHolder, int position) {
        // Suppress any unchecked warnings since it is up to the subclass to guarantee
        // compatibility of their view holders with the item holder at the corresponding position
        viewHolder.bindItemView(mItemHolders.get(position));
        viewHolder.setOnItemClickedListener(mOnItemClickedListener);
        viewHolder.setOnItemLongClickedListener(mOnItemLongClickedListener);
    }

    @Override
    public void onViewRecycled(ItemViewHolder viewHolder) {
        viewHolder.setOnItemClickedListener(null);
        viewHolder.setOnItemLongClickedListener(null);
        viewHolder.recycleItemView();
    }

    /**
     * Callback interface for when an item changes and should be re-bound.
     */
    public interface OnItemChangedListener {

        /**
         * Invoked by {@link ItemHolder#notifyItemChanged()}.
         *
         * @param itemHolder the item holder that has changed
         */
        void onItemChanged(ItemHolder<?> itemHolder);
    }

    /**
     * Callback interface for handling when an item is clicked.
     */
    public interface OnItemClickedListener {

        /**
         * Invoked by {@link ItemViewHolder#notifyItemClicked(int)}
         *
         * @param viewHolder the {@link ItemViewHolder} containing the view that was clicked
         * @param id         the unique identifier for the click action that has occurred
         */
        void onItemClicked(ItemViewHolder<?> viewHolder, int id);
    }

    /**
     * Callback interface for handling when an item is long clicked.
     */
    public interface OnItemLongClickedListener {

        /**
         * Invoked by {@link ItemViewHolder#notifyItemLongClicked(int)}
         *
         * @param viewHolder the {@link ItemViewHolder} containing the view that was clicked
         * @param id         the unique identifier for the click action that has occurred
         */
        void onItemLongClicked(ItemViewHolder<?> viewHolder, int id);
    }

    /**
     * Base class for wrapping an item for compatibility with an {@link ItemHolder}.
     * <p/>
     * An {@link ItemHolder} serves as bridge between the model and view layer; subclassers should
     * implement properties that fall beyond the scope of their model layer but are necessary for
     * the view layer. Properties that should be persisted across dataset changes can be
     * preserved via the {@link #onSaveInstanceState(Bundle)} and
     * {@link #onRestoreInstanceState(Bundle)} methods.
     * <p/>
     * Note: An {@link ItemHolder} can be used by multiple {@link ItemHolder} and any state changes
     * should simultaneously be reflected in both UIs.  It is not thread-safe however and should
     * only be used on a single thread at a given time.
     *
     * @param <T> the item type wrapped by the holder
     */
    public static abstract class ItemHolder<T> {

        /**
         * The item held by this holder.
         */
        public final T item;

        /**
         * Globally unique id corresponding to the item.
         */
        public final long itemId;

        /**
         * Listeners to be invoked by {@link #notifyItemChanged()}.
         */
        private final List<OnItemChangedListener> mOnItemChangedListeners = new ArrayList<>();

        /**
         * Designated constructor.
         *
         * @param item   the {@link T} item to be held by this holder
         * @param itemId the globally unique id corresponding to the item
         */
        public ItemHolder(T item, long itemId) {
            this.item = item;
            this.itemId = itemId;
        }

        /**
         * @return the unique identifier for the view that should be used to represent the item,
         * e.g. the layout resource id.
         */
        public abstract int getItemViewType();

        /**
         * Adds the listener to the current list of registered listeners if it is not already
         * registered.
         *
         * @param listener the listener to add
         */
        public final void addOnItemChangedListener(OnItemChangedListener listener) {
            if (!mOnItemChangedListeners.contains(listener)) {
                mOnItemChangedListeners.add(listener);
            }
        }

        /**
         * Removes the listener from the current list of registered listeners.
         *
         * @param listener the listener to remove
         */
        public final void removeOnItemChangedListener(OnItemChangedListener listener) {
            mOnItemChangedListeners.remove(listener);
        }

        /**
         * Invokes {@link OnItemChangedListener#onItemChanged(ItemHolder)} for all listeners added
         * via {@link #addOnItemChangedListener(OnItemChangedListener)}.
         */
        public final void notifyItemChanged() {
            for (OnItemChangedListener listener : mOnItemChangedListeners) {
                listener.onItemChanged(this);
            }
        }

        /**
         * Called to retrieve per-instance state when the item may disappear or change so that
         * state can be restored in {@link #onRestoreInstanceState(Bundle)}.
         * <p/>
         * Note: Subclasses must not maintain a reference to the {@link Bundle} as it may be
         * reused for other items in the {@link ItemHolder}.
         *
         * @param bundle the {@link Bundle} in which to place saved state
         */
        public void onSaveInstanceState(Bundle bundle) {
            // for subclassers
        }

        /**
         * Called to restore any per-instance state which was previously saved in
         * {@link #onSaveInstanceState(Bundle)} for an item with a matching {@link #itemId}.
         * <p/>
         * Note: Subclasses must not maintain a reference to the {@link Bundle} as it may be
         * reused for other items in the {@link ItemHolder}.
         *
         * @param bundle the {@link Bundle} in which to retrieve saved state
         */
        public void onRestoreInstanceState(Bundle bundle) {
            // for subclassers
        }
    }

    /**
     * Base class for a reusable {@link RecyclerView.ViewHolder} compatible with an
     * {@link ItemViewHolder}. Provides an interface for binding to an {@link ItemHolder} and later
     * being recycled.
     */
    public static class ItemViewHolder<T extends ItemHolder<?>> extends RecyclerView.ViewHolder {

        /**
         * The current {@link ItemHolder} bound to this holder.
         */
        private T mItemHolder;

        /**
         * The current {@link OnItemClickedListener} associated with this holder.
         */
        private OnItemClickedListener mOnItemClickedListener;

        /**
         * The current {@link OnItemLongClickedListener} associated with this holder.
         */
        private OnItemLongClickedListener mOnItemLongClickedListener;

        /**
         * Designated constructor.
         *
         * @param itemView the item {@link View} to associate with this holder
         */
        public ItemViewHolder(View itemView) {
            super(itemView);
        }

        /**
         * @return the current {@link ItemHolder} bound to this holder, or {@code null} if unbound
         */
        public final T getItemHolder() {
            return mItemHolder;
        }

        /**
         * Binds the holder's {@link #itemView} to a particular item.
         *
         * @param itemHolder the {@link ItemHolder} to bind
         */
        public final void bindItemView(T itemHolder) {
            mItemHolder = itemHolder;
            onBindItemView(itemHolder);
        }

        /**
         * Called when a new item is bound to the holder. Subclassers should override to bind any
         * relevant data to their {@link #itemView} in this method.
         *
         * @param itemHolder the {@link ItemHolder} to bind
         */
        protected void onBindItemView(T itemHolder) {
            // for subclassers
        }

        /**
         * Recycles the current item view, unbinding the current item holder and state.
         */
        public final void recycleItemView() {
            mItemHolder = null;
            mOnItemClickedListener = null;
            mOnItemLongClickedListener = null;
            onRecycleItemView();
        }

        /**
         * Called when the current item view is recycled. Subclassers should override to release
         * any bound item state and prepare their {@link #itemView} for reuse.
         */
        @SuppressWarnings("EmptyMethod")
        protected void onRecycleItemView() {
            // for subclassers
        }

        /**
         * Sets the current {@link OnItemClickedListener} to be invoked via
         * {@link #notifyItemClicked}.
         *
         * @param listener the new {@link OnItemClickedListener}, or {@code null} to clear
         */
        public final void setOnItemClickedListener(OnItemClickedListener listener) {
            mOnItemClickedListener = listener;
        }

        /**
         * Sets the current {@link OnItemLongClickedListener} to be invoked via
         * {@link #notifyItemLongClicked}.
         *
         * @param listener the new {@link OnItemLongClickedListener}, or {@code null} to clear
         */
        public final void setOnItemLongClickedListener(OnItemLongClickedListener listener) {
            mOnItemLongClickedListener = listener;
        }

        /**
         * Called by subclasses to invoke the current {@link OnItemClickedListener} for a
         * particular click event so it can be handled at a higher level.
         *
         * @param id the unique identifier for the click action that has occurred
         */
        public final void notifyItemClicked(int id) {
            if (mOnItemClickedListener != null) {
                mOnItemClickedListener.onItemClicked(this, id);
            }
        }

        /**
         * Called by subclasses to invoke the current {@link OnItemLongClickedListener} for a
         * particular long click event so it can be handled at a higher level.
         *
         * @param id the unique identifier for the long click action that has occurred
         */
        public final void notifyItemLongClicked(int id) {
            if (mOnItemLongClickedListener != null) {
                mOnItemLongClickedListener.onItemLongClicked(this, id);
            }
        }

        /**
         * Factory interface used by {@link ItemAdapter} for creating new {@link ItemViewHolder}.
         */
        public interface Factory {
            /**
             * Used by {@link ItemAdapter#createViewHolder(ViewGroup, int)} to make new
             * {@link ItemViewHolder} for a given view type.
             *
             * @param parent   the {@code ViewGroup} that the {@link ItemViewHolder#itemView} will
             *                 be attached
             * @param viewType the unique id of the item view to create
             * @return a new initialized {@link ItemViewHolder}
             */
            ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType);
        }
    }
}
