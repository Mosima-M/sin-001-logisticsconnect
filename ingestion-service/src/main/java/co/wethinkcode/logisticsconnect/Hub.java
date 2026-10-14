package co.wethinkcode.logisticsconnect;

public class Hub {
    private String hubId;
    private String province;
    private String sortingCenter;
    private Boolean active;

    public Hub(String hubId, String province, String sortingCenter, Boolean active){
        this.hubId = hubId;
        this.province = province;
        this.sortingCenter = sortingCenter;
        this.active = active;
    }

    public String getHubId(){
        return hubId;
    }

    public String getProvince(){
        return province;
    }

    public String getSortingCenter(){
        return sortingCenter;
    }

    public Boolean getActive(){
        return active;
    }

}
