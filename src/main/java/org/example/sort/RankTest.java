package org.example.sort;

import java.util.Arrays;

public class RankTest {

	public static void main(String[] args) {
		//冒泡排序
		int[] arr = { 1, 3, 5, 7, 9, 2, 4, 6, 8, 0 };
		bubbleSort(arr);
	    System.out.println();
		// 插入排序
		int[] arr2 = { 3, 1, 5, 7, 9, 2, 4, 6, 8, 0 };
		insertSort(arr2);
	    System.out.println();
		//选择排序
		int[] arr3 = { 3, 1, 5, 7, 9, 2, 4, 6, 8, 0 };
		selectSort(arr3);
		System.out.println();

		//快速排序

		int[] arr4 = { 3, 1, 5, 7, 9, 2, 4, 6, 8, 0 };
		Arrays.sort(arr4);
	}


	private static void selectSort(int[] arr3) {
		for (int i = 0; i < arr3.length; i++){
			for (int j = i; j < arr3.length; j++){
				if (arr3[i] > arr3[j]){
					int temp = arr3[i];
					arr3[i] = arr3[j];
					arr3[j] = temp;
				}
			}
		}
		Arrays.stream(arr3).forEach(System.out::print);
	}

	private static void insertSort(int[] arr2) {
		for (int i = 1; i < arr2.length; i++) {
			for (int j = i; j > 0; j--){
				if (arr2[j] < arr2[j - 1]){
					int temp = arr2[j];
					arr2[j] = arr2[j - 1];
					arr2[j - 1] = temp;
				}
			}

		}
		Arrays.stream(arr2).forEach(System.out::print);
	}

	private static void bubbleSort(int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr.length; j++) {
				if (arr[i] < arr[j]) {
					int temp = arr[i];
					arr[i] = arr[j];
					arr[j] = temp;
				}

			}
		}
		Arrays.stream(arr).forEach(System.out::print);
	}
}
