package com.example.tmdt.service.userinfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.tmdt.domain.Role;
import com.example.tmdt.domain.User;
import com.example.tmdt.service.UploadService;
import com.example.tmdt.service.UserService;


@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService{
    private final UserService userService;
    private final UploadService uploadService;

    public CustomOAuth2UserService(UserService userService, UploadService uploadService) {
        this.userService = userService;
        this.uploadService=uploadService;
    }

    public MultipartFile urlToMultipartFile(String imageUrl, String filename) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();

            return new MultipartFile() {
                @Override
                public String getName() {
                    return filename;
                }

                @Override
                public String getOriginalFilename() {
                    return filename;
                }

                @Override
                public String getContentType() {
                    return "image/jpeg";
                }

                @Override
                public boolean isEmpty() {
                    return bytes.length == 0;
                }

                @Override
                public long getSize() {
                    return bytes.length;
                }

                @Override
                public byte[] getBytes() {
                    return bytes;
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public void transferTo(File dest) throws IOException {
                    Files.write(dest.toPath(), bytes);
                }
            };
        }
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // call api
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // get provider
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // Process oAuth2User or map it to your local user database
        String email = (String) attributes.get("email");
        String fullName = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");

        // Tạo tên file gốc (độc nhất để tránh trùng)
        String fileName = email.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";

        String savedFileName="";
        


        
        System.out.println("User email: " + email + fullName);

        Role userRole = this.userService.getRoleByName("USER");

        if (email != null) {
            User user = this.userService.getUserByEmail(email);
            if (user == null) {
                try {
                    MultipartFile avatarFile = urlToMultipartFile(avatarUrl, fileName);
                    savedFileName = this.uploadService.handleSaveUploadFile(avatarFile, "avatars");
                } catch (IOException e) {
                    e.printStackTrace(); // TODO: log lỗi
                    savedFileName = "default-google.png"; // fallback
                }
            
                // Tạo user mới
                User oUser = new User();
                oUser.setEmail(email);
                oUser.setFullName(fullName);
                oUser.setProvider("GOOGLE");
            
                // nếu có avatar upload thì dùng, không thì default
                oUser.setAvatar(savedFileName != null ? savedFileName : "default-google.png");
            
            
                // Gán role mặc định (VD: STUDENT)
                oUser.setRole(userRole); // userRole lấy từ DB

            
                this.userService.handleSaveUser(oUser);
            }
        }


        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole.getName())),
                oAuth2User.getAttributes(),
                "email");
    }


    
}
