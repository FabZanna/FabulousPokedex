package com.fabulouszanna.pokedex.ui.pokemon

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fabulouszanna.pokedex.R
import com.fabulouszanna.pokedex.databinding.PokemonListBinding
import com.fabulouszanna.pokedex.repo.PokemonViewModel
import com.fabulouszanna.pokedex.ui.filters.FilterDialog
import com.fabulouszanna.pokedex.utilities.RecyclerViewCustomItemDecoration
import kotlinx.android.synthetic.main.pokemon_list.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class PokemonListFragment : Fragment() {
    private val viewModel: PokemonViewModel by viewModel()
    private lateinit var binding: PokemonListBinding
    private var currentGenFilter = "all"
    private var currentTypeFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = PokemonListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.pokemon_search_menu, menu)
        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.searchPokemon).actionView as SearchView
        searchView.apply {
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            isIconifiedByDefault = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextChange(name: String?): Boolean {
                    searchByName(name)
                    return true
                }

                override fun onQueryTextSubmit(p0: String?): Boolean {
                    return false
                }
            })
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PokemonAdapter(layoutInflater) {
            Toast.makeText(requireContext(), "Clicked ${it.name}", Toast.LENGTH_LONG).show()
        }.apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    binding.pokemonRv.scrollToPosition(0)
                }
            })
        }

        binding.pokemonRv.apply {
            setAdapter(adapter)
            layoutManager = GridLayoutManager(context, 2)
            addItemDecoration(
                RecyclerViewCustomItemDecoration(requireContext())
            )
        }

        viewModel.pokemons.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.pokemons)

            when {
                state.pokemons.isEmpty() -> {
                    binding.emptyRecyclerView.visibility = View.VISIBLE
                }
            }
        }
        binding.emptyRecyclerView.visibility = View.GONE

        filter_fab.setOnClickListener {
            navToFilters()
        }
    }

    private fun navToFilters() {
        FilterDialog(
            onGenFilterClicked = ::onGenFilterClicked,
            onTypeFilterClicked = ::onTypeFilterClicked
        )
            .show(requireActivity().supportFragmentManager, "")
    }

    private fun searchByName(pokemonName: String?) {
        pokemonName?.let {
            viewModel.filtered(it, "all", "all")
        }
    }

    private fun onGenFilterClicked(gen: String) {
        val generation =
            if (gen != "all") gen.take(3).toLowerCase(Locale.ROOT) + gen.takeLast(1) else gen
        currentGenFilter = generation
        viewModel.filtered("", generation, currentTypeFilter)
    }

    private fun onTypeFilterClicked(type: String) {
        currentTypeFilter = type
        viewModel.filtered("", currentGenFilter, type)
    }
}