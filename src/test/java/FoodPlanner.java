import models.*;
import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.http.*;
import wasapi.WasapiUtilities;
import wasapi.WasapiClient;

public class FoodPlanner extends WasapiUtilities {

    FoodPlannerServices petStoreServices = new WasapiClient.Builder()
            .logRequestBody(true)
            .logHeaders(true)
            .build(FoodPlannerServices.class);

    public SimpleMessageResponseModel signUp(UserSignUpModel userSignUpModel){
        log.info("Signing up a new user");
        Call<SimpleMessageResponseModel> signUpCall = petStoreServices.signUp(userSignUpModel);
        return perform(signUpCall, true, true);
    }

    public UserAuthResponseModel signIn(UserAuthRequestModel userAuthRequestModel){
        log.info("Signing in...");
        Call<UserAuthResponseModel> signInCall = petStoreServices.signIn(userAuthRequestModel);
        return perform(signInCall, true, true);
    }

    static class Auth extends WasapiUtilities {
        FoodPlannerServices.Authorized petStoreServicesAuth;

        public Auth(String authorisationToken){
            petStoreServicesAuth= new WasapiClient.Builder()
                    .headers(
                            new Headers.Builder()
                                    .add("Authorization", "Bearer " + authorisationToken)
                                    .build()
                    )
                    .logRequestBody(true)
                    .logHeaders(true)
                    .build(FoodPlannerServices.Authorized.class);
        }

        public GetUserResponseModel addFood(GetUserResponseModel.Food foodModel){
            log.info("Adding food for the user...");
            Call<GetUserResponseModel> checkCall = petStoreServicesAuth.addFood(foodModel);
            return perform(checkCall, true, true);
        }

        public SimpleMessageResponseModel deleteUserWithId(String userId){
            log.info("Deleting the user with id: " + userId);
            Call<SimpleMessageResponseModel> deleteUserCall = petStoreServicesAuth.deleteUserWithId(userId);
            return perform(deleteUserCall, true, true);
        }

        public SimpleMessageResponseModel deleteUserWithUsername(String username){
            log.info("Deleting the user named " + username);
            Call<SimpleMessageResponseModel> deleteUserCall = petStoreServicesAuth.deleteUserWithUsername(username);
            return perform(deleteUserCall, true, true);
        }

        public GetUserResponseModel getUser(){
            log.info("Acquiring the user...");
            Call<GetUserResponseModel> getUserCall = petStoreServicesAuth.getUser();
            return perform(getUserCall, true, true);
        }

        public SimpleMessageResponseModel logout(){
            log.info("Logging out the user...");
            Call<SimpleMessageResponseModel> logoutUserCall = petStoreServicesAuth.logout();
            return perform(logoutUserCall, true, true);
        }
    }

    public interface FoodPlannerServices {

        String BASE_URL = "http://localhost:5001/";

        @POST("/api/auth/signin")
        Call<UserAuthResponseModel> signIn(@Body UserAuthRequestModel userAuthRequestModel);

        @POST("/api/auth/signup")
        Call<SimpleMessageResponseModel> signUp(@Body UserSignUpModel userSignUpModel);

        interface Authorized {

            String BASE_URL = "http://localhost:5001/";

            @POST("/api/user/add-food")
            Call<GetUserResponseModel> addFood(@Body GetUserResponseModel.Food foodModel);

            @DELETE("/api/auth/{userId}/delete")
            Call<SimpleMessageResponseModel> deleteUserWithId(@Path("userId") String userId);

            @DELETE("/api/auth/user/{username}/delete")
            Call<SimpleMessageResponseModel> deleteUserWithUsername(@Path("username") String username);

            @GET("/api/user")
            Call<GetUserResponseModel> getUser();

            @GET("/api/logout")
            Call<SimpleMessageResponseModel> logout();
        }
    }

}
