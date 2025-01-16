package com.springboot.manage_users.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.manage_users.service.ProfileImgService;
import com.springboot.manage_users.service.UserService;
import com.springboot.manage_users.dto.BulkUserCreationResponse;
import com.springboot.manage_users.dto.ProjectUserDto;
import com.springboot.manage_users.dto.UserDTO;
import com.springboot.manage_users.dto.UserInformationDto;
import com.springboot.manage_users.entity.ProfileImg;
import com.springboot.manage_users.entity.Users;
import com.springboot.manage_users.repository.UsersRepository;

@RestController
@RequestMapping("/users")
public class UsersController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private UsersRepository userRepo;
	
	@Autowired
    private ProfileImgService profileImgService;

	@PostMapping("/newUser")
	public ResponseEntity<String> createUser(
	    @RequestParam("firstName") String firstName,
	    @RequestParam("lastName") String lastName,
	    @RequestParam("email") String email,
	    @RequestParam("phone") String phone,
	    @RequestParam("username") String username,
	    @RequestParam("password") String password,
	    @RequestParam("employeeId") String employeeId,
	    @RequestParam("role") String role,
	    @RequestParam("organizationUnit") String orgUnit,
	    @RequestParam("department") String department,
	    @RequestParam("designation") String designation,
	    @RequestParam("sendmail") boolean sendmail,
	    @RequestParam(value = "file", required = false) MultipartFile file
	) {
	    try {
	        // Create the user object
	        Users user = new Users();
	        user.setFirst_name(firstName);
	        user.setLast_name(lastName);
	        user.setEmail(email);
	        user.setPhone(phone);
	        user.setUsername(username);
	        user.setPassword(password);
	        user.setEmployee_id(employeeId);
	        user.setRole(role);
	        user.setDepartment(department);
	        user.setOrganization_unit(orgUnit);
	        user.setDesignation(designation);

	        String status = userService.createUser(user, sendmail);

	        if (!"Success".equals(status)) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(status);
	        }

	        if (file != null) {
	            try {
	                String profileImgId = profileImgService.uploadProfileImg(file, user.getUserid());
	                Optional<Users> usertoUpdate = userService.getUserById(user.getUserid());
	                if (usertoUpdate.isPresent()) {
	                    Users existingUser = usertoUpdate.get();

	                    existingUser.setProfile(profileImgId);
	                    userRepo.save(existingUser);
	                    }
	                
	            } catch (Exception e) {
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("User created but failed to upload profile image: " + e.getMessage());
	            }
	        }

	        return ResponseEntity.status(HttpStatus.OK).body("User created successfully");

	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body("Error creating user: " + e.getMessage());
	    }
	}


	@PostMapping("/bulkUsers")
	public ResponseEntity<Object> createUsers(@RequestBody List<Users> userRequests) {
	    try {
	        BulkUserCreationResponse response = userService.createUsers(userRequests);

	        if (response.getFailedCount() == 0) {
	            return ResponseEntity.ok(
	                String.format("All %d users created successfully.", response.getSuccessfulCount())
	            );
	        } else if (response.getSuccessfulCount() == 0) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	        } else {
	            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
	        }
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body("Error: " + e.getMessage());
	    }
	}
	
	@GetMapping("/getAllUsers")
    public ResponseEntity<?> getAllUsers() {
        List<UserDTO> userList = userService.getAllUsers();
        if (!userList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(userList);
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No users found");
        }
    }
	
	@GetMapping("/getUsersByIds")
	public ResponseEntity<?> getUsersByIds(@RequestBody List<String> userIds) {
	    if (userIds == null || userIds.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User IDs list is empty or null");
	    }

	    List<UserDTO> userDTOList = userService.getUsersByIds(userIds);

	    if (!userDTOList.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.OK).body(userDTOList);
	    } else {
	        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No users found for the provided IDs");
	    }
	}
	@GetMapping("/getProjectUsers")
	public ResponseEntity<?> getAllProjectUsers(
	    @RequestParam(required = false) String searchTerm,
	    @RequestParam(required = false) List<String> userIds
	) {
	    List<ProjectUserDto> userList = userService.getAllProjectUsers(searchTerm, userIds);
	    if (!userList.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.OK).body(userList);
	    } else {
	        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No users found for the provided search term or criteria");
	    }
	}


	@GetMapping("/getUser")
	public ResponseEntity<UserInformationDto> getUserById(@RequestParam("userId") String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Optional<Users> userData = userService.getUserById(userId);

        if (userData.isPresent()) {
            Users user = userData.get();
            byte[] profileImage = null;

            // Fetch the profile image
            Optional<ProfileImg> profileImg = profileImgService.getProfileImg(userId);
            if (profileImg.isPresent()) {
                profileImage = profileImg.get().getGrp_data();
            }

            UserInformationDto userDto = new UserInformationDto(
                    user.getUserid(),
                    user.getFirst_name(),
                    user.getLast_name(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmployee_id(),
                    user.getRole(),
                    user.getOrganization_unit(),
                    user.getDepartment(),
                    user.getDesignation(),
                    user.getCreated_at(),
                    user.getLast_login(),
                    user.getStatus(),
                    profileImage
            );

            return ResponseEntity.ok(userDto);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

	@PutMapping("/update")
	public ResponseEntity<?> updateUser(
	        @RequestParam("userid") String userId,
	        @RequestParam("first_name") String firstName,
	        @RequestParam("last_name") String lastName,
	        @RequestParam("email") String email,
	        @RequestParam("phone") String phone,
	        @RequestParam("employee_id") String employeeId,
	        @RequestParam("role") String role,
	        @RequestParam("organization_unit") String orgUnit,
	        @RequestParam("department") String department,
	        @RequestParam("designation") String designation,
	        @RequestParam("remove") boolean remove,
	        @RequestParam(value = "file", required = false) MultipartFile file
	) {
	    Optional<Users> existingUserOpt = userService.getUserById(userId);

	    if (existingUserOpt.isPresent()) {
	        Users user = existingUserOpt.get();
	        user.setFirst_name(firstName);
	        user.setLast_name(lastName);
	        user.setEmail(email);
	        user.setPhone(phone);
	        user.setRole(role);
	        user.setEmployee_id(employeeId);
	        user.setDepartment(department);
	        user.setDesignation(designation);
	        user.setOrganization_unit(orgUnit);

	        System.err.println("Remove : "+ remove);
	        
	        boolean updateStatus = userService.updateUser(user);

	        if (updateStatus) {
	        	
	        	if(remove)
	        	{
	        		profileImgService.deleteProfileImg(userId);
	        	}
	        	
	            if (file != null && !file.isEmpty()) {
	                try {
	                    String profileImgId = profileImgService.updateProfileImg(file, userId);
	                    Optional<Users> userToUpdate = userService.getUserById(user.getUserid());
	                    if (userToUpdate.isPresent()) {
	                        Users existingUser = userToUpdate.get();
	                        existingUser.setProfile(profileImgId);
	                        userRepo.save(existingUser);
	                    }
	                	} catch (Exception e) {
	                		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                            .body("User updated, but failed to upload profile image: " + e.getMessage());
	                	}
	            	}
	            	return ResponseEntity.status(HttpStatus.OK).body("User updated successfully");
	        	} else {
	        		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update user");
	        	}
	    	} else {
	    		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
	    	}
	}
	
	@PutMapping("/updateCredentials")
	public ResponseEntity<?> updateCredential(@RequestBody Users data)
	{
		try{
			userService.updateCredential(data);
        	return ResponseEntity.status(HttpStatus.OK).body("User credentials updated successfully");
		}
		catch(Exception e)
		{
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update user credentials");
		}
		
	}
	
	@PutMapping("/updateStatus")
	public ResponseEntity<?> updateStatus(@RequestBody Users data)
	{
		try{
			userService.updateStatus(data);
        	return ResponseEntity.status(HttpStatus.OK).body("User status updated successfully");
		}
		catch(Exception e)
		{
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update user status");
		}
		
	}
	@PostMapping("/changePassword")
	public ResponseEntity<?> sendPasswordMail(@RequestBody Map<String, String> changeData) {
	    try {
	        String name = changeData.get("name");
	        String email = changeData.get("email");
	        String url = changeData.get("url");

	        if (name == null || email == null || url == null) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required fields");
	        }

	        userService.sendPasswordResetEmail(email, name, url);
	        return ResponseEntity.status(HttpStatus.OK).body("Mail sent successfully");
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to send password reset mail");
	    }
	}

	@GetMapping("/getName")
	public ResponseEntity<?> getName(@RequestParam String userId) {
	    try {
	        String fullName = userService.getName(userId);
	        
	        return ResponseEntity.ok(fullName);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("Error retrieving user name: " + e.getMessage());
	    }
	}

}
