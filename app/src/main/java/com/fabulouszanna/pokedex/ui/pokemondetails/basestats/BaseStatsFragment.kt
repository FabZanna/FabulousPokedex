package com.fabulouszanna.pokedex.ui.pokemondetails.basestats

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fabulouszanna.pokedex.R
import com.fabulouszanna.pokedex.databinding.FragmentBaseStatsBinding
import com.fabulouszanna.pokedex.model.PokemonModel
import com.fabulouszanna.pokedex.model.pokemontypes.PokemonWeaknesses
import com.fabulouszanna.pokedex.ui.ability.AbilityFragment
import com.fabulouszanna.pokedex.utilities.RecyclerViewCustomItemDecoration
import com.fabulouszanna.pokedex.utilities.extractColorResourceFromType
import com.fabulouszanna.pokedex.utilities.toPx
import kotlinx.android.synthetic.main.fragment_base_stats.*

class BaseStatsFragment(
    private val pokemon: PokemonModel,
    private val onPopBackStack: () -> Unit
) : Fragment() {
    private lateinit var binding: FragmentBaseStatsBinding
    private val pokemonBaseStats = mutableMapOf<String, Int>()
    private var pokemonColor: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentBaseStatsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pokemonColor = extractColorResourceFromType(requireContext(), pokemon.type1)
        populateStats()
        populateAbilities()
        populateWeaknesses()
        wireNavigation()
        getNewStats()
    }

    private fun populateStats(currentStats: Map<String, Int>? = null) {
        val stats = resources.getStringArray(R.array.stats)
        // var total = 0
        binding.apply {
            stats.forEachIndexed { index, stat ->
                val container = statContainer.getChildAt(index) as LinearLayout
                val statName = container.getChildAt(0) as TextView
                val statValueTextView = container.getChildAt(1) as TextView
                val statValueProgressBar = container.getChildAt(2) as ProgressBar
                statValueProgressBar.progressTintList = ColorStateList.valueOf(pokemonColor)
                val statValue = if (currentStats == null) {
                    when (index) {
                        0 -> pokemon.stats?.hp
                        1 -> pokemon.stats?.attack
                        2 -> pokemon.stats?.defense
                        3 -> pokemon.stats?.specialAtk
                        4 -> pokemon.stats?.specialDef
                        5 -> pokemon.stats?.speed
                        else -> throw IllegalArgumentException("Stat with index $index does not exist")
                    }
                } else {
                    // Stats have been passed from the calculator
                    currentStats.maxByOrNull { it.value }?.value?.let {
                        statValueProgressBar.max = it
                    }
                    currentStats[stat].toString()
                }

                if (currentStats == null) {
                    pokemonBaseStats[stat] = statValue!!.toInt()
                }

                statName.text = stat
                statValueTextView.text = statValue
                statValueProgressBar.progress = statValue!!.toInt()

                // total += statValue.toInt()
            }

            // val totalString = "Total: $total"
            // statsTotal.text = totalString
        }
    }

    private fun View.applyStroke() {
        val gradientDrawable = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 8.toPx().toFloat()
            setStroke(2.toPx(), pokemonColor)
        }
        this.background = gradientDrawable
    }

    private fun populateAbilities() {
        // val color = extractColorResourceFromType(requireContext(), pokemon.type1)
        val abilities = pokemon.abilities
        binding.apply {
            abilities[0].let { ability ->
                ability1.text = ability
                ability1.applyStroke()
                ability1.setOnClickListener { abilityDetails(ability) }
            }
            abilities.getOrNull(1)?.let { ability ->
                ability2.visibility = View.VISIBLE
                ability2.text = ability
                ability2.applyStroke()
                ability2.setOnClickListener { abilityDetails(ability) }
            }
            pokemon.hiddenAbility?.let { ability ->
                val background = hiddenAbility.getChildAt(0)
                val hiddenAbilityTextView = hiddenAbility.getChildAt(1) as TextView
                background.backgroundTintList = ColorStateList.valueOf(pokemonColor)
                hiddenAbilityTextView.text = ability
                hiddenAbility.visibility = View.VISIBLE
                hiddenAbility.applyStroke()
                hiddenAbility.setOnClickListener { abilityDetails(ability) }
            }
        }
    }

    private fun abilityDetails(abilityName: String) {
        AbilityFragment(abilityName, pokemonColor).show(
            requireActivity().supportFragmentManager,
            ""
        )
    }

    private fun populateWeaknesses() {
        val pokemonWeaknesses = PokemonWeaknesses(pokemon)
        val recyclerViewMap = mapOf(
            weaknessRecyclerView to pokemonWeaknesses.getWeaknesses(),
            resistanceRecyclerView to pokemonWeaknesses.getResistances(),
            immunitiesRecyclerView to pokemonWeaknesses.getImmunities()
        )
        binding.apply {
            recyclerViewMap.forEach { (rv, weaknessMap) ->
                val weaknessAdapter = BaseStatsAdapter(layoutInflater, weaknessMap)
                if (weaknessAdapter.itemCount > 0) {
                    rv.apply {
                        setHasFixedSize(true)
                        adapter = weaknessAdapter
                        addItemDecoration(
                            RecyclerViewCustomItemDecoration(
                                startEndOffset = 8,
                                topBottomOffset = 4
                            )
                        )
                        extendLastOddElement(weaknessAdapter)
                    }.also {
                        val cardParent = it.parent.parent as CardView
                        cardParent.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun RecyclerView.extendLastOddElement(adapter: BaseStatsAdapter) {
        val gridLayoutManager = GridLayoutManager(requireContext(), 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (adapter.getItemViewType(position) == 0) {
                        return 1
                    }
                    return 2
                }
            }
        }
        this.layoutManager = gridLayoutManager
    }

    private fun wireNavigation() {
        binding.apply {
            calculateStats.backgroundTintList = ColorStateList.valueOf(pokemonColor)
            calculateStats.setOnClickListener {
                val bundle = bundleOf(
                    "pokemonStats" to pokemonBaseStats,
                    "pokemonColor" to pokemonColor,
                    "popBack" to onPopBackStack
                )
                findNavController().navigate(R.id.calculateStats, bundle)
            }
        }
    }

    private fun getNewStats() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<HashMap<String, Int>>(
            "result"
        )?.observe(viewLifecycleOwner) {
            populateStats(it)
        }
    }
}