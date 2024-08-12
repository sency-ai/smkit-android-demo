package com.example.smkitdemoapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.smkitdemoapp.databinding.ExerciseItemBinding
import com.example.smkitdemoapp.models.DemoExercise

interface ExerciseClickEvent {
    fun exercise(exercise: DemoExercise)
}

class SelectExerciseAdapter(
    private val exercise: ExerciseClickEvent
) : RecyclerView.Adapter<SelectExerciseAdapter.ViewHolder>() {

    private val list = mutableListOf<DemoExercise>()

    fun addItems(list: List<DemoExercise>) {
        this.list.clear()
        this.list.addAll(list)
    }

    fun clearChooices() {
        list.forEach { exercise ->
            exercise.count = 0
        }
         notifyItemRangeChanged(0, list.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ExerciseItemBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding, exercise)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    class ViewHolder(
        private val binding: ExerciseItemBinding,
        private val exerciseEvent: ExerciseClickEvent
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: DemoExercise) {
            binding.exerciseName.text = exercise.name
            binding.counter.text = exercise.count.toString()

            binding.root.setOnClickListener {
                val count = exercise.count + 1
                binding.counter.text = count.toString()
                exercise.count = count
                exerciseEvent.exercise(exercise)
            }
        }
    }
}