package com.mang0.mindcleardemo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mang0.mindcleardemo.databinding.FragmentFirstBinding

// Fragment: Uygulamanın ilk ekranda gösterdiği default sayfa
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        // Aslı, bu fragmanda aklımdasın ❤️

        return binding.root
    }

    // View hazır olduğunda buton tıklamalarını ayarlar
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    // Fragment view yok edilirken binding’i temizler
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
