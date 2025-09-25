package com.example.tmdt.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.Order;
import com.example.tmdt.domain.Role;
import com.example.tmdt.domain.User;
import com.example.tmdt.domain.dto.RegisterDTO;
import com.example.tmdt.repository.RoleRepository;
import com.example.tmdt.repository.UserRepository;



@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public String handleHello() {
        return "hello from service";
    }

    public List<User> getAllUser() {
        return this.userRepository.findAll();
    }

    public Page<User> getAllUserPagination(Pageable page) {
        return this.userRepository.findAll(page);
    }

    public List<User> getAllUserByEmail(String email) {
        return this.userRepository.findOneByEmail(email);
    }

    public List<Order> getOrdersSortedById(User user) {
        List<Order> orders = new ArrayList<>();
        if (user.getOrders() != null) {
            orders = user.getOrders(); // Chuyển thành List nếu là Set
            orders.sort(Comparator.comparing(Order::getId).reversed()); // Sắp xếp giảm dần theo id
        }

        return orders;
    }

    public User handleSaveUser(User user) {
        User eric = this.userRepository.save(user);
        return eric;
    }

    public void saveUser(User user) {
        this.userRepository.save(user);
    }

    public User getUserById(long id) {
        return this.userRepository.findById(id);
    }

    public void deleteAUser(long id) {
        this.userRepository.deleteById(id);
    }

    public Role getRoleByName(String name) {
        return this.roleRepository.findByName(name);
    }

    public User registerDTOtoUser(RegisterDTO registerDTO) {
        User user = new User();
        user.setFullName(registerDTO.getFirstName() + " " + registerDTO.getLastName());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(registerDTO.getPassword());
        return user;
    }

    public boolean checkEmailExist(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public User getUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public long countUsers() {
        return this.userRepository.count();
    }

}
