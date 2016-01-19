package model;

public class HospitalShortInfo {
    private int id;
    private String doctorIdFromStaff;
    private String hospitalName;

    public HospitalShortInfo(int id, String doctorIdFromStaff, String hospitalName) {
        this.id = id;
        this.doctorIdFromStaff = doctorIdFromStaff;
        this.hospitalName = hospitalName;
    }

    public int getId() {
        return id;
    }

    public String getDoctorIdFromStaff() {
        return doctorIdFromStaff;
    }

    public String getHospitalName() {
        return hospitalName;
    }
}
