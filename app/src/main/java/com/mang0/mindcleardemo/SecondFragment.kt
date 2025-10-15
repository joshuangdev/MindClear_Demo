package com.mang0.mindcleardemo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mang0.mindcleardemo.databinding.FragmentSecondBinding

/**
 * Bu fragment, uygulamada ikinci ekran olarak kullanılır.
 * Kullanıcı buradan önceki FirstFragment'e geri dönebilir.
 */
class SecondFragment : Fragment() {

    // View binding için nullable değişken
    private var _binding: FragmentSecondBinding? = null

    // View binding'in güvenli versiyonu, yalnızca onCreateView - onDestroyView arasında geçerli
    private val binding get() = _binding!!

    // Fragment'ın görünümü oluşturulurken çağrılır
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Layout'u bağlayıp root view'u döndür
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Fragment'ın view'ları oluşturulduktan sonra çağrılır
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Butona tıklandığında FirstFragment'e geri dön
        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    // Fragment view yok edilirken binding referansını null yaparak memory leak önle
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
