package com.document.immigrantvault.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.databinding.ItemPersonCardBinding;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;
import com.document.immigrantvault.util.StatusHelper;

import java.util.ArrayList;
import java.util.List;

public class PersonCardAdapter extends RecyclerView.Adapter<PersonCardAdapter.ViewHolder> {

    public interface OnPersonClickListener {
        void onPersonClick(Person person);
    }

    private final List<Person> items = new ArrayList<>();
    private OnPersonClickListener listener;

    public void setItems(List<Person> persons) {
        items.clear();
        if (persons != null) {
            items.addAll(persons);
        }
        notifyDataSetChanged();
    }

    public void setOnPersonClickListener(OnPersonClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonCardBinding binding = ItemPersonCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonCardBinding binding;

        ViewHolder(ItemPersonCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Person person) {
            String initial = person.name != null && !person.name.isEmpty()
                    ? person.name.substring(0, 1).toUpperCase() : "?";
            binding.avatarText.setText(initial);
            binding.personName.setText(person.name);
            binding.relationshipChip.setText(EnumLabels.relationship(person.relationship));
            StatusHelper.applyVisaStatusChip(binding.statusChip, person.visaEndDate, itemView.getContext());

            if (person.dateOfBirth != null) {
                binding.dateOfBirth.setText(
                        itemView.getContext().getString(R.string.label_date_of_birth)
                                + ": " + DateUtils.formatDate(person.dateOfBirth));
                binding.dateOfBirth.setVisibility(View.VISIBLE);
            } else {
                binding.dateOfBirth.setVisibility(View.GONE);
            }

            if (person.currentVisaType != null && !person.currentVisaType.isEmpty()) {
                binding.visaType.setText(person.currentVisaType);
            } else {
                binding.visaType.setText(itemView.getContext().getString(R.string.status_no_visa));
            }

            String start = DateUtils.formatDate(person.visaStartDate);
            String end = DateUtils.formatDate(person.visaEndDate);
            binding.visaDates.setText(start + " – " + end);
            binding.daysRemaining.setText(DateUtils.daysUntilLabel(person.visaEndDate));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPersonClick(person);
                }
            });
        }
    }
}
