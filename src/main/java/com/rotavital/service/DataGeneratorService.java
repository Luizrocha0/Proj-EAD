package com.rotavital.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DataGeneratorService {

    private final String[] BASE_MEDICATIONS = {
        "Dipirona", "Paracetamol", "Ibuprofeno", "Amoxicilina", "Azitromicina",
        "Omeprazol", "Losartana", "Atenolol", "Metformina", "Simvastatina"
    };


    public List<String> generateData(int size) {
        List<String> data = new ArrayList<>(size);
        Random random = new Random(42); 

        for (int i = 0; i < size; i++) {
            String base = BASE_MEDICATIONS[random.nextInt(BASE_MEDICATIONS.length)];
            
            if (random.nextDouble() < 0.20) {
                base = introduceTypo(base, random);
            }
            
            data.add(base);
        }

        return data;
    }

    private String introduceTypo(String word, Random random) {
        if (word.length() < 3) return word;
        
        int type = random.nextInt(3);
        int pos = random.nextInt(word.length() - 1) + 1; 
        
        StringBuilder sb = new StringBuilder(word);
        
        switch (type) {
            case 0: 
                sb.insert(pos, word.charAt(pos));
                break;
            case 1: 
                sb.deleteCharAt(pos);
                break;
            case 2: 
                sb.setCharAt(pos, (char)(word.charAt(pos) + 1));
                break;
        }
        
        return sb.toString();
    }
}
