/**
 * Created by Vasily Efimov on 05.10.2016.
 */

import static java.lang.System.out;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

//Количество типов купюр строго ограничено
enum billType {ONE, THREE, FIVE, TEN, TWFW, FIFTY, HND, FHND, TSND, FTSND}

//Контейнер для купюры
class bill{
	//Количество купюр
    public int number;
	//Достоинство
    public int mult;

    bill(int mult) {
        number = 0;
        this.mult = mult;
    }
}

public class ATM {
    public static void main(String[] args) {
        ATM_machine bankomat = new ATM_machine();
        boolean end_flag = false;
		/*
			В бесконечном цикле считываются команды пользователя.
			Некорректные команды игнорируются полностью.
			Команда считывается как строка, а затем разбивается на отдельные "слова",
			разделённые пробельными символами.
		*/
        while (!end_flag) {
			//Тот самый префикс
            out.print(">");
            String user_in = System.console().readLine();
            String[] command_parts = user_in.split("[ \t]+");
            switch (command_parts[0].toLowerCase()) {
                case "put":
                        if (command_parts.length == 3) {
                            billType T;
							/*
								Пытаемся преобразовать слово в целое число
								Если преобразование успешно, преобразуем его в тип купюры 
								с помощью специальной функции - дешифратора (таких функций несколько)
							*/
                            try {
                                T = ATM_machine.getType(Integer.parseInt(command_parts[1]));
                            }
                            catch (NumberFormatException e) {
                                break;
                            }
                            int count;
                            try {
                                count = Integer.parseInt(command_parts[2]);
                            }
                            catch (NumberFormatException e) {
                                break;
                            }
                            if (T != null && count > 0) {
                                out.println(bankomat.put(T, count));
                            }
                        }
                        break;
                case "get":
                        if (command_parts.length == 2) {
                            int amount;
                            try {
                                amount = Integer.parseInt(command_parts[1]);
                            }
                            catch (NumberFormatException e) {
                                break;
                            }
                            if (amount > 0) {
                                out.println(bankomat.get(amount));
                            }
                        }
                        break;
                case "dump": out.println(bankomat.dump());
                    break;
                case "state": out.println(bankomat.state() + " рублей");
                    break;
                case "quit": out.println("Bye, have a great time!");
					//Выполняем условие выхода из цикла
                    end_flag = true;
                    break;
            }
        }
    }
}
//Класс банкомата, в этом задании не нужно создавать много банкоматов, но можно
class ATM_machine {
    //Хранилище купюр
    private Map<billType, bill> storage = new EnumMap<>(billType.class);
	
    ATM_machine() {
        for (billType T: billType.values())
        {
			//Обход всех типов купюр, дешифратор на основе типа генерирует множитель
            storage.put(T,new bill(produceMult(T)));
        }
    }
	// Поместить купюры в банкомат
    public String put(billType D, int count) {
        String result = "всего ";
        storage.get(D).number += count;

        result = result + state() + " рублей";
        return result;
    }
	// Выдать купюры из банкомата
    public String get(int amount) {
        int rem = amount - state();
        // Если запрошено больше, чем есть, или столько же, выдаём все деньги
        if (rem >= 0) {
            String result = storage.values().stream()
					//Фильтруем коллекцию, работаем только с теми купюрами, которые есть
                    .filter(a -> a.number > 0)
					//Сортировка
                    .sorted((a, b) -> b.mult - a.mult)
					// Преобразуем в строки					
                    .map(a -> "" + a.mult + "=" + a.number + ",")
					//Свертка множества строк в одну строку
                    .reduce("", (a,b) -> a + b);
			// Очищаем содержимое, так как все купюры должны быть "выданы"
            storage.values().stream().forEach(a -> a.number = 0);
			//Если выводить было нечего, возвращаем соответствующее сообщение
            if (result.isEmpty()) {
                return "невозможно выдать сумму";
            }
            result = result + " всего " + (amount - rem);
            if(rem > 0)
                result = result + "\n" + "без " + rem;
            return result;
        }
        //Иначе нужно считать сколько и чего выдать
        else{
            String result = "";
            rem = amount;
			//Получаем итератор на сортированное по убыванию множество значений коллекции
            Iterator it = storage.values().stream().
                    sorted((a, b) -> b.mult - a.mult).
                    iterator(); 
			/*
				Проход по всему сортированному множеству
				Если можно выдать часть (или всю сумму) купюрами текущего типа
				Производится выдача некоторого количества купюр
				Банкомат пытается выдать сумму наиболее крупными купюрами
			*/
            while (it.hasNext()) {
                bill curr = (bill) it.next();
                int curr_count = rem / curr.mult;
                if (curr_count > 0 && curr.number > 0) {
                    if (curr.number >= curr_count) {
                        result = result + curr.mult + "=" + curr_count + ",";
                        rem = rem - (curr_count*curr.mult);
                        curr.number -= curr_count;
                    }
					//Если купюры подходят для выдачи суммы, но их недостаточно
                    else {
                        result = result + curr.mult + "=" + curr.number + ",";
                        rem = rem - (curr.number*curr.mult);
                        curr.number = 0;
                    }
                }
            }
            if (result.isEmpty()) {
                return "невозможно выдать сумму";
            }
            if (rem > 0)
            {
                result = result + " всего " + (amount - rem)
                        + "\n" + "без " + rem;
            }
            else result = result + " всего " + amount;
            return result;
        }
    }
    //Вывод полного состояния банкомата
    public String dump() {
        return storage.values().stream()
				//Сортируем элементы коллекции по убыванию достоинства купюры
                .sorted((a, b) -> b.mult - a.mult)
				// Преобразуем в строки
                .map(a -> "" + a.mult + " " + a.number)
				//Свертка множества строк в одну строку
                .reduce("",(a,b) -> a + b + "\n");
    }
    //Вывод суммы денег в банкомате
    public int state() {
        return storage.values()
                .stream()
				// Множество пар [достоинство, количество] преобразуются в их произведения
                .map(b -> b.mult*b.number)
				//Суммирование всех произведений
                .reduce(0, (a,b) -> a + b);
    }
	//Дешифратор из типа купюры в достоинство (множитель)
    private int produceMult(billType T) {
        switch (T) {
            case ONE: return 1;
            case THREE: return 3;
            case FIVE: return 5;
            case TEN: return 10;
            case TWFW: return 25;
            case FIFTY: return 50;
            case HND: return 100;
            case FHND: return 500;
            case TSND: return 1000;
            case FTSND: return 5000;
            default: return 0;
        }
    }
	// Дешифроатор из множителя в тип
    static billType getType(int type) {
        switch (type) {
            case 1: return billType.ONE;
            case 3: return billType.THREE;
            case 5: return billType.FIVE;
            case 10: return billType.TEN;
            case 25: return billType.TWFW;
            case 50: return billType.FIFTY;
            case 100: return billType.HND;
            case 500: return billType.FHND;
            case 1000: return billType.TSND;
            case 5000: return billType.FTSND;
            default: return null;
        }
    }
}