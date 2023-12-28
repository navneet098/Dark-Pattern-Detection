package com.example.safebrowser

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.safebrowser.databinding.FragmentBrowseBinding
import kotlin.time.times


class BrowseFragment (private var urlNew:String) : Fragment() {


    lateinit var binding: FragmentBrowseBinding
    private var darkPatternFound = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view=inflater.inflate(R.layout.fragment_browse,container,false)
        binding= FragmentBrowseBinding.bind(view)

        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onResume() {
        super.onResume()

        val mainRef=requireActivity() as MainActivity
        binding.webView.apply {
            settings.javaScriptEnabled=true
            settings.setSupportZoom(true)
            settings.builtInZoomControls=true
            settings.displayZoomControls=false
            webViewClient= object: WebViewClient(){
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    mainRef.binding.topSearchBar.text=SpannableStringBuilder(url)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        // Load the URL in the WebView
                        view?.loadUrl(url)
                        return true // Indicate that the URL has been handled
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    extractAndPassText()

//                    binding.webView.evaluateJavascript("document.body.textContent") { text ->
//
//                        processText(text)
//                    }



                }
            }
            webChromeClient=object: WebChromeClient(){
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    binding.webView.visibility=View.GONE
                    binding.customView.visibility=View.VISIBLE
                    binding.customView.addView(view)
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    binding.webView.visibility=View.VISIBLE
                    binding.customView.visibility=View.GONE
                }
            }
            when{
                URLUtil.isValidUrl(urlNew)->loadUrl(urlNew)
                urlNew.contains(".com",ignoreCase = true)->loadUrl(urlNew)
                else->loadUrl("https://www.google.com/search?q=$urlNew")
            }

        }
    }

    private fun extractAndPassText(){
        binding.webView.evaluateJavascript("(function () {\n" +
                "  const walker = document.createTreeWalker(\n" +
                "    document.body,\n" +
                "    NodeFilter.SHOW_TEXT,\n" +
                "    null,\n" +
                "    false\n" +
                "  );\n" +
                "\n" +
                "  let node;\n" +
                "  const visibleText = [];\n" +
                "\n" +
                "  while ((node = walker.nextNode())) {\n" +
                "    const parentElement = node.parentElement;\n" +
                "\n" +
                "    // Check if the parent element is visible\n" +
                "    if (\n" +
                "      parentElement &&\n" +
                "      (parentElement.offsetWidth > 0 ||\n" +
                "        parentElement.offsetHeight > 0 ||\n" +
                "        (parentElement.getClientRects().length > 0 &&\n" +
                "          parentElement.getClientRects()[0].width > 0 &&\n" +
                "          parentElement.getClientRects()[0].height > 0))\n" +
                "    ) {\n" +
                "      visibleText.push(node.textContent.trim());\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  return visibleText.join(' ');\n" +
                "}\n)();")
        { html ->
            println("Printing Here Start");
            val chunks = html.chunkString(45)
            println("Printing Here Processed");
            println("Printing Here end");
            processText(chunks)
            //println(html)
            // code here
        }

    }

    private fun processText(chunks: List<String>) {
        val mainActivity = requireActivity() as MainActivity
        val maxChunkSize = 50
        var darkcnt=1;
        var nondark=1;

        for (item in chunks) {
            println("chunk is "+item)
            val result = mainActivity.performPrediction(item)
            if (result == "Dark Pattern") {
                darkPatternFound = true
                darkcnt++; // Toast.makeText(requireContext(), "$result for "+ item, Toast.LENGTH_LONG).show()
            }
            else
                nondark++;
        }
        //checking ratio but not perfect
        println(darkcnt*100/(nondark+darkcnt))
        println(nondark*100/(nondark+darkcnt))

        if(darkcnt*100/(nondark+darkcnt)>=5)
            Toast.makeText(requireContext(), "Dark Detected", Toast.LENGTH_LONG).show()
        else
            Toast.makeText(requireContext(), "Clean Page", Toast.LENGTH_LONG).show()

        // Dark pattern already found, no need to search further



//        for (sentence in sentences) {
//            if (!darkPatternFound) { // Check if a dark pattern is already found
//                val potentialChunk = "$sentence".trim()
//
//                val result = mainActivity.performPrediction(potentialChunk)
//                if (result == "Dark Pattern") {
//                    darkPatternFound = true
//                    Toast.makeText(requireContext(), "Predicted = $result", Toast.LENGTH_LONG).show()
//                    break
//                }
//            } else {
//                break // Dark pattern already found, no need to search further
//            }
//        }


    }
    //chunking those scrapped text , bad Time complexity but okay
    fun String.chunkString(maxLength: Int): List<String> {
        val chunks = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < this.length) {
            var endIndex = minOf(startIndex + maxLength, this.length)
            while (endIndex < this.length && this[endIndex - 1] !in listOf(' ', '.')) {
                endIndex--
            }
            chunks.add(this.substring(startIndex, endIndex).trim())
            startIndex = endIndex
            while (startIndex < this.length && this[startIndex] == ' ') {
                startIndex++
            }
        }

        return chunks
    }




}
