package com.example.madadgarapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.madadgarapp.R
import com.example.madadgarapp.databinding.FragmentLoginBinding
import com.example.madadgarapp.utils.AuthManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val authManager: AuthManager by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        
        
        // Observe authentication state
        viewLifecycleOwner.lifecycleScope.launch {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthManager.AuthState.Loading -> {
                        // Show loading indicator
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is AuthManager.AuthState.Authenticated -> {
                        // Navigate to main screen
                        binding.progressBar.visibility = View.GONE
                        navigateToMain()
                    }
                    is AuthManager.AuthState.Error -> {
                        // Show error
                        binding.progressBar.visibility = View.GONE
                        showError(state.message)
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    
    
    private fun navigateToMain() {
        // Replace with your navigation logic
        Toast.makeText(requireContext(), "Sign in successful!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = LoginFragment()
    }
}
