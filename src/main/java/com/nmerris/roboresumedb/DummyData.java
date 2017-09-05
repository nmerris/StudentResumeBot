package com.nmerris.roboresumedb;

import com.nmerris.roboresumedb.models.*;
import com.nmerris.roboresumedb.repositories.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

// loads a bunch of data into repos for testing, or any reason you like
public class DummyData {

    public static void load(CourseRepo courseRepo, PersonRepo personRepo, WorkExperienceRepo workExperienceRepo,
                            SkillRepo skillRepo, EducationRepo educationRepo) {

//        // generate some random numbers
//        Random random = new Random();
//        int workExpRand = random.nextInt(4) + 1; // 1 - 4
//        int skillRand = random.nextInt(5); // 0 - 4
//        int edRand = random.nextInt(4) + 2; // 2 - 5
//        long edGradYearRand = 1930 + random.nextInt(87); // 1930 - 2016
//        int numCoursesRand = random.nextInt(7); // 0 - 6
//        int courseRand = random.nextInt(10); // 0 - 9 (must be same range as total number of courses)

        Set<Course> courses = new HashSet<>();


        // create 10 courses
        for(int i = 1; i <= 10; i++) {
            Course course = new Course();
            course.setTitle("CourseTitle" + i);
            course.setInstructor("Professor" + i);
            course.setCredits(i % 6 + 1);
            courseRepo.save(course);
            courses.add(course);
        }


        for(int i = 1; i <= 21; i++) {
            // generate some random numbers
            Random random = new Random();
            int workExpRand = random.nextInt(4) + 1; // 1 - 4
            int skillRand = random.nextInt(5); // 0 - 4
            int edRand = random.nextInt(4) + 2; // 2 - 5
            int numCoursesRand = random.nextInt(9); // 0 - 8


            Person person = new Person();
            person.setNameFirst("FirstName" + i);
            person.setNameLast("LastName" + i);
            person.setEmail("email" + i + "@domain.com");
            personRepo.save(person);

            for(int j = 0; j < workExpRand; j++) {
                WorkExperience workExperience = new WorkExperience();
                workExperience.setCompany("Company" + i + "-" + j);
                workExperience.setDateStart(new Date());
                workExperience.setJobTitle("JobTitle" + i + "-" + j);
                workExperience.setDutyOne("Duty" + i + "-" + j);
                workExperience.setMyPerson(person);
                workExperienceRepo.save(workExperience);
            }

            for(int j = 0; j < skillRand; j++) {
                Skill skill = new Skill();
                skill.setSkill("Skill" + i + "-" + j);
                skill.setRating("Expert");
                skill.setMyPerson(person);
                skillRepo.save(skill);
            }

            for(int j = 0; j < edRand; j++) {
                long edGradYearRand = 1930 + random.nextInt(87); // 1930 - 2016
                EducationAchievement ed = new EducationAchievement();
                ed.setGraduationYear(edGradYearRand);
                ed.setMajor("Major" + i + "-" + j);
                ed.setSchool("School" + i + "-" + j);
                ed.setMyPerson(person);
                educationRepo.save(ed);
            }

            // assign courses to the student in a stochastically fantastic manner
            for(int j = 0; j < numCoursesRand; j++) {
                int courseRand = random.nextInt(10); // 0 - 9 (must be same range as total number of courses)
                Set<Course> toAdd = new HashSet<>();
                toAdd.add((Course)courses.toArray()[courseRand]);
                person.addCourses(toAdd);
                personRepo.save(person);
            }

//
//
// Course course = new Course();
//                course.setTitle("CourseTitle" + i);
//                course.setInstructor("Professor" + i);
//                course.setCredits(i % 6 + 1);
//                courseRepo.save(course);
//                person.addCourse(course);
//                personRepo.save(person);
//            }

        }

    }

}
