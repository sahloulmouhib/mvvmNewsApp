package com.androiddevs.mvvmnewsapp.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androiddevs.mvvmnewsapp.R
import com.androiddevs.mvvmnewsapp.adapters.NewsAdapter
import com.androiddevs.mvvmnewsapp.ui.activity.NewsActivity
import com.androiddevs.mvvmnewsapp.ui.NewsViewModel
import com.androiddevs.mvvmnewsapp.util.Constants.Companion.QUERY_PAGE_SIZE
import com.androiddevs.mvvmnewsapp.util.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.androiddevs.mvvmnewsapp.util.Resource

import kotlinx.android.synthetic.main.fragment_breaking_news.*
import kotlinx.android.synthetic.main.fragment_search_news.*


import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchNewsFragment : Fragment(R.layout.fragment_search_news) {
    lateinit var sortedBy:String
    lateinit var viewModel: NewsViewModel
    lateinit var newsAdapter : NewsAdapter
    val TAG="serachNewsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        viewModel=(activity as NewsActivity).viewModel
        setupRecyclerView()




        spSortBy.onItemSelectedListener =object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) { sortedBy=when(position)
            {
                1 ->"relevancy"
                2->"popularity"
                else ->"publishedAt"
            }

                //Toast.makeText(activity!!,"You selected ${parent?.getItemAtPosition(position).toString()}",Toast.LENGTH_LONG).show()


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        newsAdapter.setOnItemClickListener {
            val bundle=Bundle().apply {
                putSerializable("article",it)
            }
            findNavController().navigate(R.id.action_searchNewsFragment_to_articleFragment,bundle)
        }




        var job: Job? = null
        etSearch.addTextChangedListener{editable ->
            job?.cancel()
            job= MainScope().launch {
                delay(SEARCH_NEWS_TIME_DELAY)
                editable?.let {
                    if(editable.toString().isNotEmpty())
                    {


                        viewModel.searchNews(editable.toString(),sortedBy)
                        //ivSearch.visibility=View.GONE
                    }
                }
            }
        }



        viewModel.searchNews.observe(viewLifecycleOwner, Observer { response ->
            when(response)
            {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let{
                            newsResponse ->  newsAdapter.differ.submitList(newsResponse.articles.toList())
                        if(newsResponse.articles.isNotEmpty())
                        {
                            ivSearch.visibility=View.GONE
                        }
                            val length=newsResponse.articles.toList().size
                        val totalPages= newsResponse.totalResults /QUERY_PAGE_SIZE +2
                        // we reached the last page of the first list of articles so we need to load other articles
                        isLastPage=viewModel.searchNewsPage == totalPages
                        if(isLastPage)
                        {
                            rvSearchNews.setPadding(0,0,0,0)
                        }
                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let{message ->
                        Toast.makeText(activity,"an Error occured :$message", Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Loading ->
                {
                    showProgressBar()
                }
            }
        })
    }



    //handling pagination
    var isLoading =false
    var isLastPage = false
    var isScrolling =false

    val scrollListener = object: RecyclerView.OnScrollListener() {
        // ctrl + o
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState== AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
            {
                isScrolling= true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager= recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition =layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount =layoutManager.childCount
            val totalItemCount =layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning =firstVisibleItemPosition >=0
            val isTotalMoreThanVisible =totalItemCount >= QUERY_PAGE_SIZE
            val shouldPaginate= isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning && isTotalMoreThanVisible && isScrolling
            if(shouldPaginate)
            {
                viewModel.searchNews(etSearch.text.toString(),sortedBy)
                isScrolling =false
            }


        }
    }




    private fun  setupRecyclerView()
    {
        newsAdapter= NewsAdapter()
        rvSearchNews.apply {
            adapter = newsAdapter
            layoutManager= LinearLayoutManager(activity)
            //pagination
            addOnScrollListener(this@SearchNewsFragment.scrollListener)
        }
    }

    private fun hideProgressBar()
    {
        paginationProgressBarSearch.visibility= View.INVISIBLE
        isLoading=false
    }

    private fun showProgressBar()
    {
        paginationProgressBarSearch.visibility= View.VISIBLE
        isLoading=true
    }
}

